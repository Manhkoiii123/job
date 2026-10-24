package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.JobRequest;
import com.manh.job.dto.request.JobSearchRequest;
import com.manh.job.dto.response.JobResponse;
import com.manh.job.service.JobService;
import com.manh.job.util.AdminGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final AdminGuard adminGuard;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @RequestHeader("X-User-Id") Long employerId,
            @RequestBody @Valid JobRequest req
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(employerId, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long viewerId
    ) throws Exception {
        jobService.getJobById(id, viewerId);
        jobService.incrementViewCount(id);
        return ResponseEntity.ok(jobService.getJobById(id, viewerId));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs(@ModelAttribute JobSearchRequest request) {
        return ResponseEntity.ok(jobService.getAllJobs(request));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<JobResponse>> getJobsByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(jobService.getJobsByCompany(companyId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId,
            @RequestBody @Valid JobRequest req
    ) throws Exception {
        return ResponseEntity.ok(jobService.updateJob(id, employerId, req));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<JobResponse> publishJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {
        return ResponseEntity.ok(jobService.publishJob(id, employerId));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<JobResponse> closeJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {
        return ResponseEntity.ok(jobService.closeJob(id, employerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {
        jobService.deleteJob(id, employerId);
        return ResponseEntity.ok(new ApiResponse("Job deleted successfully", true));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<JobResponse>> getAllJobsAdmin(
            @RequestHeader("X-User-Id") Long adminId
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        return ResponseEntity.ok(jobService.getAllJobsAdmin());
    }
}
