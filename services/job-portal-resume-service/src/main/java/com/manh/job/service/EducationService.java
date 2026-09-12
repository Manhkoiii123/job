package com.manh.job.service;

import com.manh.job.payload.request.AddEducationRequest;
import com.manh.job.payload.response.EducationResponse;

import java.util.List;

public interface EducationService {
    EducationResponse addEducation(Long resumeId, Long candidateId, AddEducationRequest req) throws Exception;

    List<EducationResponse> getEducations(Long resumeId);

    EducationResponse updateEducation(Long educationId,
                                      Long resumeId, Long candidateId,
                                      AddEducationRequest req) throws Exception;

    void deleteEducation(Long educationId, Long resumeId, Long candidateId) throws Exception;
}
