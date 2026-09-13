package com.manh.job.controller;

import com.manh.job.domain.ApplicationStatus;
import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.CompanyApplicationFilterRequest;
import com.manh.job.dto.request.CreateApplicationRequest;
import com.manh.job.dto.request.UpdateApplicationStatusRequest;
import com.manh.job.dto.request.WithdrawApplicationRequest;
import com.manh.job.dto.response.ApplicationResponse;
import com.manh.job.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
	    @RequestHeader("X-User-Id") Long candidateId,
	    @RequestBody @Valid CreateApplicationRequest req
    ) throws Exception {
	return ResponseEntity.status(HttpStatus.CREATED)
		.body(applicationService.createApplication(candidateId, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
	    @PathVariable Long id
    ) throws Exception {
	return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
	    @RequestHeader("X-User-Id") Long candidateId
    ) {
	return ResponseEntity.ok(applicationService.getMyApplications(candidateId));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForJob(
	    @PathVariable Long jobId
    ) {
	return ResponseEntity.ok(applicationService.getApplicationsForJob(jobId));
    }

    @GetMapping("/company")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForCompany(
	    @RequestHeader("X-User-Id") Long employerId,
	    @ModelAttribute CompanyApplicationFilterRequest request
    ) {
	return ResponseEntity.ok(
		applicationService.getApplicationsForCompany(employerId, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
	    @PathVariable Long id,
	    @RequestHeader("X-User-Id") Long employerId,
        @RequestBody @Valid UpdateApplicationStatusRequest req
    ) throws Exception {
	return ResponseEntity.ok(applicationService.updateStatus(id, employerId, req.getStatus()));
    }

    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<ApplicationResponse> withdrawApplication(
	    @PathVariable Long id,
	    @RequestHeader("X-User-Id") Long candidateId,
	    @RequestBody @Valid WithdrawApplicationRequest req
    ) throws Exception {
	return ResponseEntity.ok(applicationService.withDraw(id, candidateId, req));
    }

    @PatchMapping("/{id}/star")
    public ResponseEntity<ApplicationResponse> toggleStar(
	    @PathVariable Long id,
	    @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {
	return ResponseEntity.ok(applicationService.toggleStar(id, employerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteApplication(
	    @PathVariable Long id,
	    @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
	applicationService.deleteApplication(id);
	return ResponseEntity.ok(new ApiResponse("Application deleted successfully", true));
    }
}
