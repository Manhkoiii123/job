package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.AddWorkExperienceRequest;
import com.manh.job.dto.response.WorkExperienceResponse;
import com.manh.job.service.WorkExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumes/{resumeId}/work-experiences")
@RequiredArgsConstructor
public class WorkExperienceController {
    private final WorkExperienceService workExperienceService;

    @PostMapping
    public ResponseEntity<WorkExperienceResponse> addWorkExperience(
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid AddWorkExperienceRequest req
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                workExperienceService.addWorkExperience(resumeId, candidateId, req)
        );
    }

    @GetMapping
    public ResponseEntity<List<WorkExperienceResponse>> getWorkExperiences(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        return ResponseEntity.ok(
                workExperienceService.getWorkExperiences(resumeId, candidateId)
        );
    }

    @PutMapping("/{experienceId}")
    public ResponseEntity<WorkExperienceResponse> updateWorkExperience(
            @PathVariable Long resumeId,
            @PathVariable Long experienceId,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid AddWorkExperienceRequest req
    ) throws Exception {
        return ResponseEntity.ok(
                workExperienceService.updateWorkExperience(
                        resumeId,
                        candidateId,
                        experienceId,
                        req
                )
        );
    }

    @DeleteMapping("/{experienceId}")
    public ResponseEntity<ApiResponse> deleteWorkExperience(
            @PathVariable Long resumeId,
            @PathVariable Long experienceId,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        workExperienceService.deleteWorkExperience(resumeId, experienceId, candidateId);
        return ResponseEntity.ok(new ApiResponse("Work experience deleted successfully", true));
    }
}
