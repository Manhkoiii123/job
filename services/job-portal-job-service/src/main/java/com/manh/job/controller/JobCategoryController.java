package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.payload.response.JobCategoryResponse;
import com.manh.job.payload.request.JobCategoryRequest;
import com.manh.job.service.JobCategoryService;
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

    @PostMapping
    public ResponseEntity<JobCategoryResponse> createCategory(
            @RequestBody @Valid JobCategoryRequest req) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobCategoryService.createCategory(req));
    }

    @GetMapping
    public ResponseEntity<List<JobCategoryResponse>> getAllCategories() {
        List<JobCategoryResponse> categories = jobCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobCategoryResponse> getCategoryById(
            @PathVariable Long id) throws Exception {
        JobCategoryResponse category = jobCategoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobCategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid JobCategoryRequest req) throws Exception {
        JobCategoryResponse updatedCategory = jobCategoryService.updateCategory(id, req);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(
            @PathVariable Long id) throws Exception {
        jobCategoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse("Category deleted successfully",true));
    }
}