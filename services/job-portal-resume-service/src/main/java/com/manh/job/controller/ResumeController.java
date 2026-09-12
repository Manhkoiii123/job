package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.PersonalInfoResponse;
import com.manh.job.dto.request.CreateResumeRequest;
import com.manh.job.dto.response.ResumeResponse;
import com.manh.job.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<ResumeResponse> createResume(
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid CreateResumeRequest req
    ) {
        return ResponseEntity.ok(resumeService.createResume(candidateId, req));
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<ResumeResponse> getResumeById(
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        return ResponseEntity.ok(resumeService.getResumeById(resumeId, candidateId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ResumeResponse>> getMyResumes(
            @RequestHeader("X-User-Id") Long candidateId
    ) {
        return ResponseEntity.ok(resumeService.getMyResumes(candidateId));
    }

    @PutMapping("/{resumeId}/personal-info")
    public ResponseEntity<ResumeResponse> updatePersonalInfo(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid PersonalInfoResponse req
    ) throws Exception {
        return ResponseEntity.ok(
                resumeService.updatePersonalInfo(resumeId, candidateId, req)
        );
    }

    @PatchMapping("/{resumeId}/summary")
    public ResponseEntity<ResumeResponse> updateSummary(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestParam String summary
    ) throws Exception {
        return ResponseEntity.ok(
                resumeService.updateSummary(resumeId, candidateId, summary)
        );
    }

    @PatchMapping("/{resumeId}/set-default")
    public ResponseEntity<ResumeResponse> setDefaultResume(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        return ResponseEntity.ok(
                resumeService.setDefaultResume(resumeId, candidateId)
        );
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<ApiResponse> deleteResume(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        resumeService.deleteResume(resumeId, candidateId);
        return ResponseEntity.ok(
                new ApiResponse("Resume deleted successfully", true)
        );
    }
}
