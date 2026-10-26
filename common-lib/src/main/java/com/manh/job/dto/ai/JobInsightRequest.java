package com.manh.job.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobInsightRequest {

    @NotBlank
    private String title;

    private String rawNotes;
    private String city;
    private String country;
    private String experienceLevel;
    private String jobType;
    private String workMode;
}
