package com.manh.job.client;

import com.manh.job.dto.response.JobResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-portal-job-service")
public interface JobClient {

    @GetMapping("/api/jobs/{jobId}")
    JobResponse getJobById(@PathVariable("jobId") Long jobId);
}
