package com.manh.job.service;

import com.manh.job.payload.request.AddResumeSkillRequest;
import com.manh.job.payload.response.ResumeSkillResponse;

import java.util.List;

public interface ResumeSkillService {
    ResumeSkillResponse addSkill(Long resumeId, Long candidateId, AddResumeSkillRequest req) throws Exception;
    List<ResumeSkillResponse> getSkills(Long resumeId);

    ResumeSkillResponse updateSkill(
            Long skillId, Long resumeId, Long candidateId, AddResumeSkillRequest req
    ) throws Exception;

    void deleteSkill(Long skillId, Long resumeId, Long candidateId) throws Exception;
}
