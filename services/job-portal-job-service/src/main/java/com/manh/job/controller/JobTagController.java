package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.payload.request.JobTagRequest;
import com.manh.job.payload.response.JobTagResponse;
import com.manh.job.service.JobTagService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/job-tags")
@RequiredArgsConstructor
public class JobTagController {

    private final JobTagService jobTagService;

    @PostMapping
    public ResponseEntity<JobTagResponse> createTag(
            @RequestBody @Valid JobTagRequest req) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobTagService.createTag(req));
    }

    @GetMapping
    public ResponseEntity<List<JobTagResponse>> getAllTags() {
        return ResponseEntity.ok(jobTagService.getAllTags());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobTagResponse> getTagById(
            @PathVariable Long id) throws Exception {
        return ResponseEntity.ok(jobTagService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobTagResponse> updateTag(
            @PathVariable Long id,
            @RequestBody @Valid JobTagRequest req) throws Exception {
        return ResponseEntity.ok(jobTagService.updateTag(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTag(
            @PathVariable Long id) throws Exception {
        jobTagService.deleteTag(id);
        return ResponseEntity.ok(new ApiResponse("Tag deleted successfully", true));
    }
}
