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
public class CoverLetterRequest {

    @NotBlank
    private String candidateName;

    @NotBlank
    private String jobTitle;

    @NotBlank
    private String companyName;

    @NotBlank
    private String resumeHighlights;

    private String tone;
}
