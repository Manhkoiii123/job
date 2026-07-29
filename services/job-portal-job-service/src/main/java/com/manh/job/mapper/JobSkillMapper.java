package com.manh.job.mapper;

import com.manh.job.modal.JobSkill;
import com.manh.job.payload.response.JobSkillResponse;

public class JobSkillMapper {
    public static JobSkillResponse toJobSkillResponse(JobSkill skill) {

        return JobSkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .slug(skill.getSlug())
                .category(skill.getCategory())
                .active(skill.getActive())
                .build();
    }
}
