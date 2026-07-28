package com.manh.job.service;

import com.manh.job.payload.request.JobRequest;
import com.manh.job.payload.response.JobResponse;
import com.manh.job.payload.request.JobSearchRequest;

import java.util.List;

public interface JobService {
    JobResponse createJob(Long employerId, JobRequest req);

    JobResponse getJobById(Long id) throws Exception;

    List<JobResponse> getAllJobs(JobSearchRequest req);

    List<JobResponse> getJobsByCompany(Long companyId);

    JobResponse updateJob(Long jobId, Long employerId, JobRequest req) throws Exception;

    JobResponse publishJob(Long jobId, Long employerId) throws Exception;

    JobResponse closeJob(Long jobId, Long employerId) throws Exception;

    void deleteJob(Long jobId, Long employerId) throws Exception;

    List<JobResponse> getAllJobsAdmin();


}
