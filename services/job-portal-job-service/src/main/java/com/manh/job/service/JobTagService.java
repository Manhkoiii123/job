package com.manh.job.service;

import com.manh.job.modal.JobTag;
import com.manh.job.payload.request.JobTagRequest;
import com.manh.job.payload.response.JobTagResponse;

import java.util.List;

public interface JobTagService {
    JobTagResponse createTag(JobTagRequest req) throws Exception;
    List<JobTagResponse> getAllTags();
    JobTagResponse getById(Long id) throws Exception;
    JobTagResponse updateTag(Long id,JobTagRequest req) throws Exception;
    void deleteTag(Long id) throws Exception;
    JobTag getTagEntityById(Long id) throws Exception;
}
