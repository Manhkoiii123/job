package com.manh.job.dto.request;

import com.manh.job.domain.AiShortListStatus;
import com.manh.job.domain.ApplicationStatus;
import lombok.Data;

@Data
public class CompanyApplicationFilterRequest {
    private Long jobId;

    private ApplicationStatus status;

    private Boolean isStarred;

    private AiShortListStatus aiShortListStatus;

    private Integer minAiScore;

    private String sortBy;
}
