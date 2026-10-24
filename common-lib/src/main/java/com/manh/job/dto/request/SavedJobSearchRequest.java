package com.manh.job.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SavedJobSearchRequest {

    private Long jobId;
    private Long companyId;
    private String keyword;
}
