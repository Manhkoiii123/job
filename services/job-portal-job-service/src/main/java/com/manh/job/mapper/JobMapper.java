package com.manh.job.mapper;

import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.JobResponse;
import com.manh.job.modal.Job;
import com.manh.job.modal.embeddable.JobLocation;
import com.manh.job.modal.embeddable.SalaryRange;

public class JobMapper {
    public static JobResponse toResponse(Job job, CompanyResponse companyResponse) {
        JobLocation loc = job.getLocation();
        SalaryRange sal =  job.getSalaryRange();
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .responsibilities(job.getResponsibilities())
                .benefits(job.getBenefits())
                .company(companyResponse)
                .employerId(job.getEmployerId())
                .category(job.getCategory() != null
                    ? JobCategoryMapper.toJobCategoryResponse(job.getCategory(), false)
                    : null)
                .skills(job.getSkills() != null
                    ? job.getSkills().stream()
                    .map(JobSkillMapper::toJobSkillResponse)
                    .collect(java.util.stream.Collectors.toSet())
                    : java.util.Set.of())
                .tags(job.getTags() != null
                    ? job.getTags().stream()
                    .map(JobTagMapper::toTagResponse)
                    .collect(java.util.stream.Collectors.toSet())
                    : java.util.Set.of())

                .address(loc != null ? loc.getAddress() : null)
                .city(loc != null ? loc.getCity() : null)
                .state(loc != null ? loc.getState() : null)
                .country(loc != null ? loc.getCountry() : null)
                .zipCode(loc != null ? loc.getZipCode() : null)

                .minSalary(sal != null ? sal.getMinSalary() : null)
                .maxSalary(sal != null ? sal.getMaxSalary() : null)
//                .currency(sal != null ? sal.getCurrency() : null)
//                .salaryPeriod(sal != null ? sal.getPeriod() : null)
//                .salaryNegotiable(sal != null ? sal.getNegotiable() : null)
//                .salaryDisclosed(sal != null ? sal.getDisclosed() : null)

                .jobType(job.getJobType())
                .workMode(job.getWorkMode())
                .experienceLevel(job.getExperienceLevel())
                .status(job.getStatus())

                .openings(job.getOpenings())
                .applicationDeadline(job.getApplicationDeadline())
                .expiresAt(job.getExpiresAt())
                .active(job.getActive())

                .viewCount(job.getViewCount())
//                .applicationCount(job.getApplicationCount())

                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .publishedAt(job.getPublishedAt())
                .closedAt(job.getClosedAt())
                .build();
    }
}
