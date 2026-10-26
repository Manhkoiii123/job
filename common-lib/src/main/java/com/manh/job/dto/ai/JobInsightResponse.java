package com.manh.job.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobInsightResponse {

    private String generatedDescription;
    private String requirements;
    private String responsibilities;
    private String benefits;
    private BigDecimal suggestedMinSalary;
    private BigDecimal suggestedMaxSalary;
    private String currency;

    @Builder.Default
    private List<String> recommendedSkills = new ArrayList<>();

    @Builder.Default
    private List<String> recommendedTasks = new ArrayList<>();
}
