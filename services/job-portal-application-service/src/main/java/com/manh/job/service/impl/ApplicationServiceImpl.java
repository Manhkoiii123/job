package com.manh.job.service.impl;

import com.manh.job.domain.ApplicationStatus;
import com.manh.job.dto.request.CompanyApplicationFilterRequest;
import com.manh.job.dto.request.CreateApplicationRequest;
import com.manh.job.dto.request.WithdrawApplicationRequest;
import com.manh.job.dto.response.ApplicationResponse;
import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.JobResponse;
import com.manh.job.dto.response.UserResponse;
import com.manh.job.mapper.ApplicationMapper;
import com.manh.job.modal.Application;
import com.manh.job.repository.ApplicationRepository;
import com.manh.job.repository.ApplicationSpecification;
import com.manh.job.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationRepository applicationRepository;
    @Override
    public ApplicationResponse createApplication(Long candidateId, CreateApplicationRequest req) throws Exception {
        if(applicationRepository.existsByCandidateIdAndJobId(candidateId,req.getJobId())) {
            throw new Exception("You have already applied");
        }
        Long companyId = 1L;
        Long employeeId = 1L;
        //  todo fetch job

        //  todo fetch resume

        Application application = ApplicationMapper.toEntity(
                req,candidateId,companyId, employeeId
        );
        Application saved = applicationRepository.save(application);
        // todo AI screening runs in a background thread
        return buildFullResponse(saved);
    }

    @Override
    public ApplicationResponse getApplicationById(Long id) throws Exception {
        Application application = getApplicationEntity(id);
        return buildFullResponse(application);
    }

    @Override
    public List<ApplicationResponse> getMyApplications(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId)
                .stream().map(
                        this::buildFullResponse
                ).toList();
    }

    @Override
    public List<ApplicationResponse> getApplicationsForJob(Long jobId) {
        return applicationRepository.findByJobId(jobId)
                .stream().map(
                        this::buildFullResponse
                ).toList();
    }

    @Override
    public List<ApplicationResponse> getApplicationsForCompany(
            Long userId,
            CompanyApplicationFilterRequest filter
    ) {
        // todo: fetch company by ownerId
        Long companyId = 1L;
        Sort sort = buildSort(filter.getSortBy());
        return  applicationRepository.findAll(ApplicationSpecification.forCompanyWithFilters(
                companyId,
                filter.getJobId(),
                filter.getStatus(),
                filter.getIsStarred(),
                filter.getAiShortListStatus(),
                filter.getMinAiScore()
        ),sort).stream().map(
                this::buildFullResponse
        ).toList();
    }

    private Sort buildSort(String sortBy) {
        if("AI_SCORE_DESC".equals(sortBy)){
            return Sort.by(Sort.Order.desc("aiScore").with(Sort.NullHandling.NULLS_LAST));
        }
        else if("AI_SCORE_ASC".equals(sortBy)){
            return Sort.by(Sort.Order.asc("aiScore").with(Sort.NullHandling.NULLS_LAST));
        }
        return Sort.by(Sort.Direction.DESC, "appliedAt");
    }

    @Override
    public ApplicationResponse updateStatus(Long applicationId,Long employerId, ApplicationStatus status) throws Exception {
        Application application = getApplicationEntity(applicationId);
        assertEmployer(application, employerId);
        if(application.getStatus()==ApplicationStatus.WITHDRAWN){
            throw new Exception("candidate have already withdrawn");
        }
        application.setStatus(status);
        Application savedApplication= applicationRepository.save(application);
        return buildFullResponse(savedApplication);
    }


    @Override
    public ApplicationResponse withDraw(Long applicationId, Long candidateId, WithdrawApplicationRequest req) throws Exception {
        Application application = getApplicationEntity(applicationId);
        assertCandidate(application,candidateId);
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setWithdrawnReason(req.getReason());
        Application savedApplication= applicationRepository.save(application);
        return buildFullResponse(savedApplication);
    }

    @Override
    public ApplicationResponse toggleStar(Long applicationId, Long employerId) throws Exception {
        Application application = getApplicationEntity(applicationId);
        assertEmployer(application, employerId);
        application.setIsStarred(!application.getIsStarred());
        Application saved = applicationRepository.save(application);
        return buildFullResponse(saved);
    }

    @Override
    public void deleteApplication(Long applicationId) throws Exception {
        Application application = getApplicationEntity(applicationId);
        assertCandidate(application, applicationId);
        applicationRepository.delete(application);
    }

    @Override
    public Application getApplicationEntity(Long id) throws Exception {
        return applicationRepository.findById(id).orElseThrow(
                ()-> new Exception("Not found")
        );
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

    public ApplicationResponse buildFullResponse(Application application) {
        //todo : fetch real data from respective microservice
        JobResponse job = JobResponse.builder().id(application.getJobId()).build();
        CompanyResponse company = CompanyResponse.builder().id(application.getCompanyId()).build();
        UserResponse candidate = UserResponse.builder().id(application.getCandidateId()).build();

        return ApplicationMapper.toResponse(
                application,
                job,
                company,
                candidate
        );
    }
}
