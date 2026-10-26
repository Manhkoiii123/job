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
public class ResumeAnalyzeRequest {

    @NotBlank
    private String resumeText;

    /** Optional target job description for tailored advice */
    private String targetJobDescription;
}
