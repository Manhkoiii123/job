package com.manh.job.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveJobRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
