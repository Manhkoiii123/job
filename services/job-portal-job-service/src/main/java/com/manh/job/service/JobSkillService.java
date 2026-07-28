package com.manh.job.service;

import com.manh.job.payload.request.JobSkillRequest;
import com.manh.job.payload.response.JobSkillResponse;
import com.manh.job.model.JobSkill;

import java.util.List;
import java.util.Set;

public interface JobSkillService {
    JobSkillResponse createSkill(JobSkillRequest req) throws Exception;

    List<JobSkillResponse> getAllSkills();

    JobSkillResponse getSkillById(Long id) throws Exception;

    JobSkillResponse updateSkill(Long id, JobSkillRequest req) throws Exception;

    void deleteSkill(Long id) throws Exception;

    Set<JobSkill> getSkillsByIds(Set<Long> ids);
}
