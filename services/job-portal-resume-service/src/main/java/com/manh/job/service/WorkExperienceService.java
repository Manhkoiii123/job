package com.manh.job.service;

import com.manh.job.modal.WorkExperience;
import com.manh.job.payload.request.AddWorkExperienceRequest;
import com.manh.job.payload.response.WorkExperienceResponse;

import java.util.List;

public interface WorkExperienceService {
    WorkExperienceResponse addWorkExperience(Long resumeId, Long candidateId, AddWorkExperienceRequest req) throws Exception;

    List<WorkExperienceResponse> getWorkExperiences(Long resumeId);

    WorkExperienceResponse updateWorkExperience(
            Long resumeId,
            Long candidateId,
            Long workExperienceId,
            AddWorkExperienceRequest req
    ) throws Exception;

    void deleteWorkExperience(Long resumeId, Long workExperienceId,Long candidateId) throws Exception;

    WorkExperience getWorkExperienceEntity(Long workExperienceId) throws Exception;
}
