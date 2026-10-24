package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.JobTagRequest;
import com.manh.job.dto.response.JobTagResponse;
import com.manh.job.service.JobTagService;
import com.manh.job.util.AdminGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-tags")
@RequiredArgsConstructor
public class JobTagController {

    private final JobTagService jobTagService;
    private final AdminGuard adminGuard;

    @PostMapping
    public ResponseEntity<JobTagResponse> createTag(
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody @Valid JobTagRequest req
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobTagService.createTag(req));
    }

    @GetMapping
    public ResponseEntity<List<JobTagResponse>> getAllTags() {
        return ResponseEntity.ok(jobTagService.getAllTags());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobTagResponse> getTagById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(jobTagService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobTagResponse> updateTag(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody @Valid JobTagRequest req
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        return ResponseEntity.ok(jobTagService.updateTag(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTag(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        jobTagService.deleteTag(id);
        return ResponseEntity.ok(new ApiResponse("Tag deleted successfully", true));
    }
}
