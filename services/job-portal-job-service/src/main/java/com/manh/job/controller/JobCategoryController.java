package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.JobCategoryRequest;
import com.manh.job.dto.response.JobCategoryResponse;
import com.manh.job.service.JobCategoryService;
import com.manh.job.util.AdminGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-categories")
@RequiredArgsConstructor
public class JobCategoryController {

    private final JobCategoryService jobCategoryService;
    private final AdminGuard adminGuard;

    @PostMapping
    public ResponseEntity<JobCategoryResponse> createCategory(
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody @Valid JobCategoryRequest req
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobCategoryService.createCategory(req));
    }

    @GetMapping
    public ResponseEntity<List<JobCategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(jobCategoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobCategoryResponse> getCategoryById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(jobCategoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobCategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody @Valid JobCategoryRequest req
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        return ResponseEntity.ok(jobCategoryService.updateCategory(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        jobCategoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse("Category deleted successfully", true));
    }
}
