package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.AddEducationRequest;
import com.manh.job.dto.response.EducationResponse;
import com.manh.job.service.EducationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/resumes/{resumeId}/educations")
@RequiredArgsConstructor
public class EducationController {
	private final EducationService educationService;

	@PostMapping
	public ResponseEntity<EducationResponse> addEducation(
			@PathVariable("resumeId") Long resumeId,
			@RequestHeader("X-User-Id") Long candidateId,
			@RequestBody @Valid AddEducationRequest req
	) throws Exception {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(educationService.addEducation(resumeId, candidateId, req));
	}

	@GetMapping
	public ResponseEntity<List<EducationResponse>> getEducations(
			@PathVariable Long resumeId
	) {
		return ResponseEntity.ok(educationService.getEducations(resumeId));
	}

	@PutMapping("/{educationId}")
	public ResponseEntity<EducationResponse> updateEducation(
			@PathVariable Long resumeId,
			@PathVariable Long educationId,
			@RequestHeader("X-User-Id") Long candidateId,
			@RequestBody @Valid AddEducationRequest req
	) throws Exception {
		return ResponseEntity.ok(
				educationService.updateEducation(educationId, resumeId, candidateId, req)
		);
	}

	@DeleteMapping("/{educationId}")
	public ResponseEntity<ApiResponse> deleteEducation(
			@PathVariable Long resumeId,
			@PathVariable Long educationId,
			@RequestHeader("X-User-Id") Long candidateId
	) throws Exception {
		educationService.deleteEducation(educationId, resumeId, candidateId);
		return ResponseEntity.ok(new ApiResponse("Education deleted successfully", true));
	}
}
