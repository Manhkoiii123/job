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
public class SemanticSearchDocument {

    @NotBlank
    private String id;

    @NotBlank
    private String text;

    private String title;
}
