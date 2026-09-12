package com.manh.job.mapper;

import com.manh.job.modal.WorkExperience;
import com.manh.job.payload.response.WorkExperienceResponse;

public class WorkExperienceMapper {
    public static WorkExperienceResponse toWorkExperienceResponse(WorkExperience exp) {
        if(exp == null) return null;

        return WorkExperienceResponse.builder()
                .id(exp.getId())
                .companyName(exp.getCompanyName())
                .companyLogoUrl(exp.getCompanyLogoUrl())
                .jobTitle(exp.getJobTitle())
                .employmentType(exp.getEmploymentType())
                .location(exp.getLocation())
                .startDate(exp.getStartDate())
                .endDate(exp.getEndDate())
                .isCurrentJob(exp.getIsCurrentJob())
                .description(exp.getDescription())
                .technologies(exp.getTechnologies())
                .displayOrder(exp.getDisplayOrder())
                .build();
    }
}
