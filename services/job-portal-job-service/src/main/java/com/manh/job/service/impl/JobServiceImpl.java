package com.manh.job.service.impl;

import com.manh.job.domain.JobStatus;
import com.manh.job.payload.request.JobRequest;
import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.payload.response.JobResponse;
import com.manh.job.mapper.JobMapper;
import com.manh.job.model.Job;
import com.manh.job.model.embeddable.JobLocation;
import com.manh.job.model.embeddable.SalaryRange;
import com.manh.job.payload.request.JobSearchRequest;
import com.manh.job.repository.JobRepository;
import com.manh.job.service.JobService;
import com.manh.job.specification.JobSpecification;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;

    @Override
    public JobResponse createJob(Long employerId, JobRequest req) {
//      fetch company by employer id
        Long companyId = 1L;
        Job job = Job.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .requirements(req.getRequirements())
                .responsibilities(req.getResponsibilities())
                .benefits(req.getBenefits())
                .companyId(companyId)
                .employerId(employerId)
//                .category(category)
//                .skills(skills)
//                .tags(tags)
                .location(buildLocation(req))
                .salaryRange(buildSalaryRange(req))
                .jobType(req.getJobType())
                .workMode(req.getWorkMode())
                .experienceLevel(req.getExperienceLevel())
                .openings(req.getOpenings() != null ? req.getOpenings() : 1)
                .applicationDeadline(req.getApplicationDeadline())
                .expiresAt(req.getExpiresAt())
                .build();

        Job savedJob = jobRepository.save(job);

        return convertToResponse(savedJob);

    }

    @Override
    public JobResponse getJobById(Long id) throws Exception {
        Job job = jobRepository.findById(id).orElseThrow(
                () -> new Exception("Job not found")
        );
        return convertToResponse(job);
    }

    @Override
    public List<JobResponse> getAllJobs(JobSearchRequest request) {
        List<Job> jobs = jobRepository.findAll(JobSpecification.build(request));
        return jobs.stream().map(
                this::convertToResponse
        ).collect(Collectors.toList());
    }

    @Override
    public List<JobResponse> getJobsByCompany(Long companyId) {
        List<Job> jobs = jobRepository.findByCompanyId(companyId);
        return jobs.stream().map(
                this::convertToResponse
        ).collect(Collectors.toList());
    }

    @Override
    public JobResponse updateJob(Long jobId, Long employerId, JobRequest req) throws Exception {
        Job job = jobRepository.findById(jobId).orElseThrow(
                () -> new Exception("Job not found")
        );
        assertEmployer(job, employerId);
        job.setTitle(req.getTitle());
        job.setDescription(req.getDescription());
        job.setRequirements(req.getRequirements());
        job.setResponsibilities(req.getResponsibilities());
        job.setBenefits(req.getBenefits());
//        job.setCategory(category);
//        job.setSkills(skills);
//        job.setTags(tags);
        job.setLocation(buildLocation(req));
        job.setSalaryRange(buildSalaryRange(req));
        job.setJobType(req.getJobType());
        job.setWorkMode(req.getWorkMode());
        job.setExperienceLevel(req.getExperienceLevel());
        job.setOpenings(req.getOpenings() != null ? req.getOpenings() : job.getOpenings());
        job.setApplicationDeadline(req.getApplicationDeadline());
        job.setExpiresAt(req.getExpiresAt());

        return convertToResponse(jobRepository.save(job));
    }

    @Override
    public JobResponse publishJob(Long jobId, Long employerId) throws Exception {
        Job job = jobRepository.findById(jobId).orElseThrow(
                () -> new Exception("Job not found")
        );
        assertEmployer(job,employerId);
        if(job.getStatus() == JobStatus.CLOSED ||  job.getStatus() == JobStatus.EXPIRED ) {
            throw new Exception("Job has been closed");
        }
        job.setStatus(JobStatus.OPEN);
        job.setPublishedAt(LocalDateTime.now());
        job.setActive(true);
        return convertToResponse(jobRepository.save(job));
    }


    @Override
    public JobResponse closeJob(Long jobId, Long employerId) throws Exception {
        Job job = jobRepository.findById(jobId).orElseThrow(
                () -> new Exception("Job not found")
        );
        assertEmployer(job,employerId);
        job.setStatus(JobStatus.CLOSED);
        job.setClosedAt(LocalDateTime.now());
        job.setActive(false);
        return convertToResponse(jobRepository.save(job));
    }

    @Override
    public void deleteJob(Long jobId, Long employerId) throws Exception {
        Job job = jobRepository.findById(jobId).orElseThrow(
                () -> new Exception("Job not found")
        );
        assertEmployer(job,employerId);
        jobRepository.delete(job);
    }

    @Override
    public List<JobResponse> getAllJobsAdmin() {
        return  jobRepository.findAll().stream().map(
                this::convertToResponse
        ).collect(Collectors.toList());
    }

    private JobResponse convertToResponse(Job savedJob) {
//        TODO: fetch company response
        CompanyResponse companyResponse =  CompanyResponse
                .builder()
                .id(savedJob.getCompanyId())
                .build();
        return JobMapper.toResponse(savedJob, companyResponse);
    }

    private SalaryRange buildSalaryRange(JobRequest req) {
        return  SalaryRange.builder()
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .build();
    }

    private JobLocation buildLocation(JobRequest req) {
        return JobLocation.builder()
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .country(req.getCountry())
                .zipCode(req.getZipCode())
                .build();
    }

    private void assertEmployer(Job job, Long employerId) throws Exception {
        if(!job.getEmployerId().equals(employerId)) {
            throw new Exception("Employer id mismatch");
        }
    }

}
