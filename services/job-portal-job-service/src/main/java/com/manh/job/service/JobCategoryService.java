package com.manh.job.service;

import com.manh.job.payload.response.JobCategoryResponse;
import com.manh.job.modal.JobCategory;
import com.manh.job.payload.request.JobCategoryRequest;

import java.util.List;

public interface JobCategoryService {
    JobCategoryResponse createCategory(JobCategoryRequest req) throws Exception;

    List<JobCategoryResponse> getAllCategories();

    JobCategoryResponse getCategoryById(Long id) throws Exception;
    JobCategoryResponse updateCategory(Long id, JobCategoryRequest req) throws Exception;

    void deleteCategory(Long id) throws Exception;

    JobCategory getCategoryEntityById(Long id) throws Exception;
}
