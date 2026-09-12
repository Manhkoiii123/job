package com.manh.job.payload.request;

import com.manh.job.domain.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddWorkExperienceRequest {
    @NotBlank(message = "company name is required")
    private String companyName;

    private String companyLogoUrl;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    private JobType employmentType;
    private String location;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isCurrentJob = false;

    private String description;
    private List<String> technologies;
    private Integer displayOrder;
}
