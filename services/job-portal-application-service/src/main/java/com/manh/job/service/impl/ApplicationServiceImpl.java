package com.manh.job.service.impl;

import com.manh.job.client.CompanyClient;
import com.manh.job.client.JobClient;
import com.manh.job.client.ResumeClient;
import com.manh.job.client.UserClient;
import com.manh.job.domain.ApplicationStatus;
import com.manh.job.domain.JobStatus;
import com.manh.job.dto.event.ApplicationStatusChangedEvent;
import com.manh.job.dto.request.CompanyApplicationFilterRequest;
import com.manh.job.dto.request.CreateApplicationRequest;
import com.manh.job.dto.request.WithdrawApplicationRequest;
import com.manh.job.dto.response.ApplicationResponse;
import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.JobResponse;
import com.manh.job.dto.response.ResumeResponse;
import com.manh.job.dto.response.UserResponse;
import com.manh.job.event.ApplicationStatusChangedPublisher;
import com.manh.job.mapper.ApplicationMapper;
import com.manh.job.modal.Application;
import com.manh.job.modal.ApplicationNote;
import com.manh.job.modal.ApplicationStatusHistory;
import com.manh.job.repository.ApplicationNoteRepository;
import com.manh.job.repository.ApplicationRepository;
import com.manh.job.repository.ApplicationSpecification;
import com.manh.job.repository.ApplicationStatusHistoryRepository;
import com.manh.job.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import feign.FeignException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationNoteRepository applicationNoteRepository;
    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final ApplicationStatusChangedPublisher applicationStatusChangedPublisher;
    private final JobClient jobClient;
    private final CompanyClient companyClient;
    private final ResumeClient resumeClient;
    private final UserClient userClient;

    @Override
    public ApplicationResponse createApplication(Long candidateId, CreateApplicationRequest req) throws Exception {
        if (applicationRepository.existsByCandidateIdAndJobId(candidateId, req.getJobId())) {
            throw new Exception("You have already applied");
        }

        JobResponse job = fetchJob(req.getJobId());
        if (job.getStatus() != JobStatus.OPEN || !Boolean.TRUE.equals(job.getActive())) {
            throw new Exception("Job is not open for applications");
        }

        ResumeResponse resume = fetchResume(req.getResumeId(), candidateId);
        if (!candidateId.equals(resume.getCandidateId())) {
            throw new Exception("Resume does not belong to you");
        }
        if (Boolean.FALSE.equals(resume.getIsActive())) {
            throw new Exception("Resume is not active");
        }

        Long companyId = resolveCompanyId(job);
        Long employerId = job.getEmployerId();
        if (employerId == null) {
            throw new Exception("Job does not have an associated employer");
        }

        Application application = ApplicationMapper.toEntity(
                req, candidateId, companyId, employerId
        );
        Application saved = applicationRepository.save(application);
        return buildFullResponse(saved);
    }

    @Override
    public ApplicationResponse getApplicationById(Long id, Long userId) throws Exception {
        Application application = getApplicationEntity(id);
        assertCanView(application, userId);
        return buildFullResponse(application);
    }

    @Override
    public List<ApplicationResponse> getMyApplications(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId)
                .stream()
                .map(this::buildFullResponse)
                .toList();
    }

    @Override
    public List<ApplicationResponse> getApplicationsForJob(Long jobId, Long employerId) throws Exception {
        JobResponse job = fetchJob(jobId);
        if (!employerId.equals(job.getEmployerId())) {
            throw new Exception("you are not the employer for this job");
        }
        return applicationRepository.findByJobId(jobId)
                .stream()
                .map(this::buildFullResponse)
                .toList();
    }

    @Override
    public List<ApplicationResponse> getApplicationsForCompany(
            Long userId,
            CompanyApplicationFilterRequest filter
    ) throws Exception {
        CompanyResponse company = fetchCompanyByOwner(userId);
        Sort sort = buildSort(filter.getSortBy());
        return applicationRepository.findAll(
                ApplicationSpecification.forCompanyWithFilters(
                        company.getId(),
                        filter.getJobId(),
                        filter.getStatus(),
                        filter.getIsStarred(),
                        filter.getAiShortListStatus(),
                        filter.getMinAiScore()
                ),
                sort
        ).stream().map(this::buildFullResponse).toList();
    }

    private Sort buildSort(String sortBy) {
        if ("AI_SCORE_DESC".equals(sortBy)) {
            return Sort.by(Sort.Order.desc("aiScore").with(Sort.NullHandling.NULLS_LAST));
        }
        if ("AI_SCORE_ASC".equals(sortBy)) {
            return Sort.by(Sort.Order.asc("aiScore").with(Sort.NullHandling.NULLS_LAST));
        }
        return Sort.by(Sort.Direction.DESC, "appliedAt");
    }

    @Override
    public ApplicationResponse updateStatus(
            Long applicationId,
            Long employerId,
            ApplicationStatus status
    ) throws Exception {
        Application application = getApplicationEntity(applicationId);
        assertEmployer(application, employerId);
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new Exception("candidate have already withdrawn");
        }
        assertEmployerAllowedStatus(status);
        ApplicationStatus previous = application.getStatus();
        application.setStatus(status);
        Application saved = applicationRepository.save(application);
        recordStatusHistory(saved, previous, status, employerId);
        publishStatusChanged(saved, previous, status);
        return buildFullResponse(saved);
    }

    private void assertEmployerAllowedStatus(ApplicationStatus status) throws Exception {
        if (status == null || status == ApplicationStatus.WITHDRAWN) {
            throw new Exception("Invalid application status");
        }
        switch (status) {
            case PENDING, REVIEWING, SHORTLISTED, INTERVIEW_SCHEDULED, REJECTED, HIRED -> {
            }
            default -> throw new Exception("Invalid application status");
        }
    }

    @Override
    public ApplicationResponse withDraw(
            Long applicationId,
            Long candidateId,
            WithdrawApplicationRequest req
    ) throws Exception {
        Application application = getApplicationEntity(applicationId);
        assertCandidate(application, candidateId);
        ApplicationStatus previous = application.getStatus();
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setWithdrawnReason(req.getReason());
        application.setWithdrawnAt(LocalDateTime.now());
        Application saved = applicationRepository.save(application);
        recordStatusHistory(saved, previous, ApplicationStatus.WITHDRAWN, candidateId);
        publishStatusChanged(saved, previous, ApplicationStatus.WITHDRAWN);
        return buildFullResponse(saved);
    }

    @Override
    public ApplicationResponse toggleStar(Long applicationId, Long employerId) throws Exception {
        Application application = getApplicationEntity(applicationId);
        assertEmployer(application, employerId);
        boolean current = Boolean.TRUE.equals(application.getIsStarred());
        application.setIsStarred(!current);
        return buildFullResponse(applicationRepository.save(application));
    }

    @Override
    public void deleteApplication(Long applicationId, Long candidateId) throws Exception {
        Application application = getApplicationEntity(applicationId);
        assertCandidate(application, candidateId);
        applicationRepository.delete(application);
    }

    @Override
    public Application getApplicationEntity(Long id) throws Exception {
        return applicationRepository.findById(id).orElseThrow(
                () -> new Exception("Not found")
        );
    }

    private void assertCanView(Application application, Long userId) throws Exception {
        if (application.getCandidateId().equals(userId)
                || application.getEmployerId().equals(userId)) {
            return;
        }
        throw new Exception("you are not allowed to view this application");
    }

    private void assertEmployer(Application application, Long employerId) throws Exception {
        if (!application.getEmployerId().equals(employerId)) {
            throw new Exception("you are not the employer for this application");
        }
    }

    private void assertCandidate(Application application, Long candidateId) throws Exception {
        if (!application.getCandidateId().equals(candidateId)) {
            throw new Exception("you are not the owner for this application");
        }
    }

    private JobResponse fetchJob(Long jobId) throws Exception {
        try {
            JobResponse job = jobClient.getJobById(jobId);
            if (job == null) {
                throw new Exception("Job not found");
            }
            return job;
        } catch (FeignException ex) {
            throw new Exception("Job not found");
        }
    }

    private ResumeResponse fetchResume(Long resumeId, Long candidateId) throws Exception {
        try {
            ResumeResponse resume = resumeClient.getResumeById(resumeId, candidateId);
            if (resume == null) {
                throw new Exception("Resume not found");
            }
            return resume;
        } catch (FeignException ex) {
            throw new Exception("Resume not found");
        }
    }

    private CompanyResponse fetchCompanyByOwner(Long ownerId) throws Exception {
        try {
            CompanyResponse company = companyClient.getCompanyByOwnerId(ownerId);
            if (company == null) {
                throw new Exception("Company not found");
            }
            return company;
        } catch (FeignException ex) {
            throw new Exception("Company not found");
        }
    }

    private Long resolveCompanyId(JobResponse job) throws Exception {
        if (job.getCompany() != null && job.getCompany().getId() != null) {
            return job.getCompany().getId();
        }
        throw new Exception("Job does not have an associated company");
    }

    private void recordStatusHistory(
            Application application,
            ApplicationStatus previous,
            ApplicationStatus next,
            Long changedBy
    ) {
        applicationStatusHistoryRepository.save(ApplicationStatusHistory.builder()
                .applicationId(application.getId())
                .previousStatus(previous)
                .newStatus(next)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build());
    }

    private void publishStatusChanged(
            Application application,
            ApplicationStatus previous,
            ApplicationStatus next
    ) {
        JobResponse job = null;
        CompanyResponse company = null;
        UserResponse candidate = null;
        try {
            job = jobClient.getJobById(application.getJobId());
        } catch (FeignException ignored) {
        }
        try {
            company = companyClient.getCompanyById(application.getCompanyId());
        } catch (FeignException ignored) {
        }
        try {
            candidate = userClient.getUserById(application.getCandidateId());
        } catch (FeignException ignored) {
        }

        ApplicationStatusChangedEvent event = ApplicationStatusChangedEvent.builder()
                .applicationId(application.getId())
                .candidateId(application.getCandidateId())
                .employerId(application.getEmployerId())
                .jobId(application.getJobId())
                .companyId(application.getCompanyId())
                .candidateEmail(candidate != null ? candidate.getEmail() : null)
                .candidateName(candidate != null ? candidate.getFullName() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .companyName(company != null ? company.getName() : null)
                .previousStatus(previous)
                .newStatus(next)
                .changedAt(LocalDateTime.now())
                .build();
        applicationStatusChangedPublisher.publish(event);
    }

    public ApplicationResponse buildFullResponse(Application application) {
        JobResponse job = null;
        CompanyResponse company = null;
        UserResponse candidate = null;
        try {
            job = jobClient.getJobById(application.getJobId());
        } catch (FeignException ignored) {
            job = JobResponse.builder().id(application.getJobId()).build();
        }
        try {
            company = companyClient.getCompanyById(application.getCompanyId());
        } catch (FeignException ignored) {
            company = CompanyResponse.builder().id(application.getCompanyId()).build();
        }
        try {
            candidate = userClient.getUserById(application.getCandidateId());
        } catch (FeignException ignored) {
            candidate = UserResponse.builder().id(application.getCandidateId()).build();
        }
        List<ApplicationNote> notes = applicationNoteRepository.findByApplicationId(application.getId());
        return ApplicationMapper.toResponse(application, notes, job, company, candidate);
    }
}
