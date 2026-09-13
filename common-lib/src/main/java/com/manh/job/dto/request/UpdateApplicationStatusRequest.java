package com.manh.job.dto.request;

import com.manh.job.domain.ApplicationStatus;
import lombok.Data;

@Data
public class UpdateApplicationStatusRequest {
    private ApplicationStatus status;
}
