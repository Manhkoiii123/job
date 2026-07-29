package com.manh.job.service;

import com.manh.job.dto.PersonalInfoResponse;
import com.manh.job.modal.Resume;
import com.manh.job.payload.request.CreateResumeRequest;
import com.manh.job.payload.response.ResumeResponse;

import java.util.List;

public interface ResumeService {
    ResumeResponse createResume(Long candidateId, CreateResumeRequest req);

    ResumeResponse getResumeById(Long resumeId, Long candidateId) throws Exception;

    List<ResumeResponse> getMyResumes(Long candidateId);

    ResumeResponse updatePersonalInfo(Long resumeId, Long candidateId, PersonalInfoResponse req) throws Exception;

    ResumeResponse updateSummary(Long resumeId, Long candidateId, String summary) throws Exception;

    ResumeResponse setDefaultResume(Long resumeId, Long candidateId) throws Exception;

    void deleteResume(Long resumeId, Long candidateId) throws Exception;

    Resume getResumeEntity(Long resumeId) throws Exception;
}
