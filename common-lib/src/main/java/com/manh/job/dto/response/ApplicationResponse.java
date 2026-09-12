package com.manh.job.dto.response;

import com.manh.job.domain.ApplicationStatus;
import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.UserResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ApplicationResponse {
    private Long id;
    private UserResponse candidate;
    private Long employerId;

    private JobResponse job;
    private CompanyResponse company;

    private ApplicationStatus status;

    // Submission content
    private Long resumeId;
    private String coverLetter;

    // Candidate preferences
    private BigDecimal expectedSalary;
    private LocalDate availableFrom;

    // Tracking
    private Boolean isRead;
    private Boolean isStarred;

    // Related data (populated on demand)
    private List<ApplicationStatusHistoryResponse> statusHistory;
    private List<InterviewResponse> interviews;
    private List<ApplicationNoteResponse> notes;
}
