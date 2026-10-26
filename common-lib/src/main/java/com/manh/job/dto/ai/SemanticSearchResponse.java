package com.manh.job.dto.ai;

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
public class SemanticSearchResponse {

    private String query;
    private String interpretedIntent;

    @Builder.Default
    private List<String> suggestedRoles = new ArrayList<>();

    @Builder.Default
    private List<String> suggestedSkills = new ArrayList<>();

    @Builder.Default
    private List<String> suggestedLocations = new ArrayList<>();

    @Builder.Default
    private List<SemanticSearchHit> hits = new ArrayList<>();
}
