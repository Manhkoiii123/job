package com.manh.job.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJobResponse {

    private Long id;
    private Long candidateId;
    private Long jobId;
    private Long companyId;
    private String notes;
    private LocalDateTime savedAt;

    private JobResponse job;
    private CompanyResponse company;
}
