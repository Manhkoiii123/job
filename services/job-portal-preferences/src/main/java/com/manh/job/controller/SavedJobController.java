package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.SaveJobRequest;
import com.manh.job.dto.request.SavedJobSearchRequest;
import com.manh.job.dto.request.UpdateSavedJobNotesRequest;
import com.manh.job.dto.response.SavedJobResponse;
import com.manh.job.service.SavedJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/saved-jobs")
@RequiredArgsConstructor
public class SavedJobController {

    private final SavedJobService savedJobService;

    @PostMapping
    public ResponseEntity<SavedJobResponse> saveJob(
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid SaveJobRequest request
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedJobService.saveJob(candidateId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<SavedJobResponse>> getMySavedJobs(
            @RequestHeader("X-User-Id") Long candidateId
    ) {
        return ResponseEntity.ok(savedJobService.getMySavedJobs(candidateId));
    }

    @GetMapping
    public ResponseEntity<List<SavedJobResponse>> searchMySavedJobs(
            @RequestHeader("X-User-Id") Long candidateId,
            @ModelAttribute SavedJobSearchRequest request
    ) {
        return ResponseEntity.ok(savedJobService.searchMySavedJobs(candidateId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedJobResponse> getSavedJobById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        return ResponseEntity.ok(savedJobService.getSavedJobById(id, candidateId));
    }

    @GetMapping("/check/{jobId}")
    public ResponseEntity<Map<String, Boolean>> isJobSaved(
            @PathVariable Long jobId,
            @RequestHeader("X-User-Id") Long candidateId
    ) {
        return ResponseEntity.ok(Map.of("saved", savedJobService.isJobSaved(candidateId, jobId)));
    }

    @PatchMapping("/{id}/notes")
    public ResponseEntity<SavedJobResponse> updateNotes(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid UpdateSavedJobNotesRequest request
    ) throws Exception {
        return ResponseEntity.ok(savedJobService.updateNotes(id, candidateId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> unsaveJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        savedJobService.unsaveJob(id, candidateId);
        return ResponseEntity.ok(new ApiResponse("Job unsaved successfully", true));
    }
}
