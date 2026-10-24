package com.manh.job.service.impl;

import com.manh.job.client.CompanyClient;
import com.manh.job.client.JobClient;
import com.manh.job.dto.request.SaveJobRequest;
import com.manh.job.dto.request.SavedJobSearchRequest;
import com.manh.job.dto.request.UpdateSavedJobNotesRequest;
import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.JobResponse;
import com.manh.job.dto.response.SavedJobResponse;
import com.manh.job.mapper.SavedJobMapper;
import com.manh.job.modal.SavedJob;
import com.manh.job.repository.SavedJobRepository;
import com.manh.job.repository.SavedJobSpecification;
import com.manh.job.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedJobServiceImpl implements SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobClient jobClient;
    private final CompanyClient companyClient;

    @Override
    public SavedJobResponse saveJob(Long candidateId, SaveJobRequest request) throws Exception {
        if (savedJobRepository.existsByCandidateIdAndJobId(candidateId, request.getJobId())) {
            throw new Exception("Job already saved");
        }

        JobResponse job = fetchJob(request.getJobId());
        Long companyId = resolveCompanyId(job);
        CompanyResponse company = fetchCompany(companyId);

        SavedJob savedJob = SavedJob.builder()
                .candidateId(candidateId)
                .jobId(request.getJobId())
                .companyId(companyId)
                .notes(request.getNotes())
                .build();

        SavedJob saved = savedJobRepository.save(savedJob);
        return SavedJobMapper.toResponse(saved, job, company);
    }

    @Override
    public SavedJobResponse getSavedJobById(Long id, Long candidateId) throws Exception {
        SavedJob savedJob = getOwnedSavedJob(id, candidateId);
        return enrichResponse(savedJob);
    }

    @Override
    public List<SavedJobResponse> getMySavedJobs(Long candidateId) {
        return savedJobRepository.findByCandidateIdOrderBySavedAtDesc(candidateId)
                .stream()
                .map(this::enrichResponseSafe)
                .toList();
    }

    @Override
    public List<SavedJobResponse> searchMySavedJobs(Long candidateId, SavedJobSearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "savedAt");
        return savedJobRepository.findAll(
                        SavedJobSpecification.forCandidateWithFilters(candidateId, request),
                        sort
                )
                .stream()
                .map(this::enrichResponseSafe)
                .toList();
    }

    @Override
    public SavedJobResponse updateNotes(
            Long id,
            Long candidateId,
            UpdateSavedJobNotesRequest request
    ) throws Exception {
        SavedJob savedJob = getOwnedSavedJob(id, candidateId);
        savedJob.setNotes(request.getNotes());
        SavedJob updated = savedJobRepository.save(savedJob);
        return enrichResponse(updated);
    }

    @Override
    public void unsaveJob(Long id, Long candidateId) throws Exception {
        SavedJob savedJob = getOwnedSavedJob(id, candidateId);
        savedJobRepository.delete(savedJob);
    }

    @Override
    public boolean isJobSaved(Long candidateId, Long jobId) {
        return savedJobRepository.existsByCandidateIdAndJobId(candidateId, jobId);
    }

    private SavedJob getOwnedSavedJob(Long id, Long candidateId) throws Exception {
        return savedJobRepository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> new Exception("Saved job not found"));
    }

    private JobResponse fetchJob(Long jobId) throws Exception {
        try {
            JobResponse job = jobClient.getJobById(jobId);
            if (job == null) {
                throw new Exception("Job not found");
            }
            return job;
        } catch (RestClientException ex) {
            throw new Exception("Job not found");
        }
    }

    private CompanyResponse fetchCompany(Long companyId) throws Exception {
        try {
            CompanyResponse company = companyClient.getCompanyById(companyId);
            if (company == null) {
                throw new Exception("Company not found");
            }
            return company;
        } catch (RestClientException ex) {
            throw new Exception("Company not found");
        }
    }

    private Long resolveCompanyId(JobResponse job) throws Exception {
        if (job.getCompany() != null && job.getCompany().getId() != null) {
            return job.getCompany().getId();
        }
        throw new Exception("Job does not have an associated company");
    }

    private SavedJobResponse enrichResponse(SavedJob savedJob) {
        JobResponse job = null;
        CompanyResponse company = null;
        try {
            job = jobClient.getJobById(savedJob.getJobId());
        } catch (RestClientException ignored) {
            // Job may have been deleted; still return saved record with denormalized ids.
        }
        try {
            company = companyClient.getCompanyById(savedJob.getCompanyId());
        } catch (RestClientException ignored) {
            // Company may have been deleted; still return saved record with denormalized ids.
        }
        return SavedJobMapper.toResponse(savedJob, job, company);
    }

    private SavedJobResponse enrichResponseSafe(SavedJob savedJob) {
        return enrichResponse(savedJob);
    }
}
