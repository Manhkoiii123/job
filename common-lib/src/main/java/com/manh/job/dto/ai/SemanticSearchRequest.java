package com.manh.job.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemanticSearchRequest {

    @NotBlank
    private String query;

    @NotEmpty
    @Builder.Default
    private List<SemanticSearchDocument> documents = new ArrayList<>();

    /** Max results to return (default 5) */
    private Integer topK;
}
