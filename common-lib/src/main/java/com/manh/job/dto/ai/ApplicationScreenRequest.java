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
public class ApplicationScreenRequest {

    @NotBlank
    private String jobTitle;

    @NotBlank
    private String jobRequirements;

    @NotBlank
    private String resumeText;

    private String coverLetter;
}
