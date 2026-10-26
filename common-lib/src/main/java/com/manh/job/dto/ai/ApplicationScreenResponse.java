package com.manh.job.dto.ai;

import com.manh.job.domain.AiShortListStatus;
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
public class ApplicationScreenResponse {

    private Integer matchScore;
    private AiShortListStatus shortListStatus;
    private String tldrSummary;
    private String coverLetterFeedback;

    @Builder.Default
    private List<String> matchedSkills = new ArrayList<>();

    @Builder.Default
    private List<String> missingSkills = new ArrayList<>();

    @Builder.Default
    private List<String> recruiterNotes = new ArrayList<>();
}
