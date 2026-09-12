package com.manh.job.service.impl;

import com.manh.job.client.CompanyClient;
import com.manh.job.domain.JobStatus;
import com.manh.job.dto.request.JobRequest;
import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.JobResponse;
import com.manh.job.mapper.JobMapper;
import com.manh.job.modal.Job;
import com.manh.job.modal.JobCategory;
import com.manh.job.modal.JobSkill;
import com.manh.job.modal.JobTag;
import com.manh.job.modal.embeddable.JobLocation;
import com.manh.job.modal.embeddable.SalaryRange;
import com.manh.job.dto.request.JobSearchRequest;
import com.manh.job.repository.JobRepository;
import com.manh.job.repository.JobTagRepository;
import com.manh.job.service.JobCategoryService;
import com.manh.job.service.JobSkillService;
import com.manh.job.service.JobService;
import com.manh.job.specification.JobSpecification;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final CompanyClient companyClient;
    private final JobCategoryService jobCategoryService;
    private final JobSkillService jobSkillService;
    private final JobTagRepository jobTagRepository;

    @Override
    public JobResponse createJob(Long employerId, JobRequest req) throws Exception {
        CompanyResponse company = companyClient.getCompanyByOwnerId(employerId);
        Job job = Job.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .requirements(req.getRequirements())
                .responsibilities(req.getResponsibilities())
                .benefits(req.getBenefits())
                .companyId(company.getId())
                .employerId(employerId)
                .category(resolveCategory(req.getCategoryId()))
                .skills(resolveSkills(req.getSkillIds()))
                .tags(resolveTags(req.getTagIds()))
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

        return convertToResponse(savedJob, company);

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
        job.setCategory(resolveCategory(req.getCategoryId()));
        job.setSkills(resolveSkills(req.getSkillIds()));
        job.setTags(resolveTags(req.getTagIds()));
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
        CompanyResponse companyResponse = companyClient.getCompanyById(savedJob.getCompanyId());
        return JobMapper.toResponse(savedJob, companyResponse);
    }

    private JobResponse convertToResponse(Job savedJob, CompanyResponse companyResponse) {
        return JobMapper.toResponse(savedJob, companyResponse);
    }

    private JobCategory resolveCategory(Long categoryId) throws Exception {
        return jobCategoryService.getCategoryEntityById(categoryId);
    }

    private Set<JobSkill> resolveSkills(Set<Long> skillIds) throws Exception {
        if (skillIds == null || skillIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<JobSkill> skills = jobSkillService.getSkillsByIds(skillIds);
        if (skills.size() != skillIds.size()) {
            throw new Exception("One or more skills were not found");
        }
        return skills;
    }

    private Set<JobTag> resolveTags(Set<Long> tagIds) throws Exception {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<JobTag> tags = new HashSet<>(jobTagRepository.findAllById(tagIds));
        if (tags.size() != tagIds.size()) {
            throw new Exception("One or more tags were not found");
        }
        return tags;
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
