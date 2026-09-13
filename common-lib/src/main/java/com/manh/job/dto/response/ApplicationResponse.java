package com.manh.job.dto.response;

import com.manh.job.domain.ApplicationStatus;
import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.UserResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private Long id;
    private UserResponse candidate;
    private Long employerId;

    private JobResponse job;
    private CompanyResponse company;

    private ApplicationStatus status;

    private Long resumeId;
    private String coverLetter;

    private BigDecimal expectedSalary;
    private LocalDate availableFrom;

    private Boolean isStarred;

    private List<ApplicationNoteResponse> notes;

    private LocalDateTime withdrawnAt;
    private String withdrawnReason;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

//    private ApplicationScreeningResponse screening;
}
