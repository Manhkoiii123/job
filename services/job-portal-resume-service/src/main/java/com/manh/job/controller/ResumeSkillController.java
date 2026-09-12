package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.payload.request.AddResumeSkillRequest;
import com.manh.job.payload.response.ResumeSkillResponse;
import com.manh.job.service.ResumeSkillService;
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
@RequestMapping("/api/resumes/{resumeId}/skills")
@RequiredArgsConstructor
public class ResumeSkillController {
	private final ResumeSkillService resumeSkillService;

	@PostMapping
	public ResponseEntity<ResumeSkillResponse> addSkill(
			@PathVariable Long resumeId,
			@RequestHeader("X-User-Id") Long candidateId,
			@RequestBody @Valid AddResumeSkillRequest req
	) throws Exception {
		return ResponseEntity.status(HttpStatus.CREATED).body(resumeSkillService.addSkill(resumeId, candidateId, req));
	}

	@GetMapping
	public ResponseEntity<List<ResumeSkillResponse>> getSkills(
			@PathVariable Long resumeId
	) {
		return ResponseEntity.ok(resumeSkillService.getSkills(resumeId));
	}

	@PutMapping("/{skillId}")
	public ResponseEntity<ResumeSkillResponse> updateSkill(
			@PathVariable Long resumeId,
			@PathVariable Long skillId,
			@RequestHeader("X-User-Id") Long candidateId,
			@RequestBody @Valid AddResumeSkillRequest req
	) throws Exception {
		return ResponseEntity.ok(
				resumeSkillService.updateSkill(skillId, resumeId, candidateId, req)
		);
	}

	@DeleteMapping("/{skillId}")
	public ResponseEntity<ApiResponse> deleteSkill(
			@PathVariable Long resumeId,
			@PathVariable Long skillId,
			@RequestHeader("X-User-Id") Long candidateId
	) throws Exception {
		resumeSkillService.deleteSkill(skillId, resumeId, candidateId);
		return ResponseEntity.ok(new ApiResponse("Resume skill deleted successfully", true));
	}
}
