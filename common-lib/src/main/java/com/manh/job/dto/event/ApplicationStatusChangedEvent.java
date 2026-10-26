package com.manh.job.dto.event;

import com.manh.job.domain.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payload gửi qua Kafka topic {@code application.status.changed}.
 * Application Service = producer; Notification Service = consumer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusChangedEvent {

    private Long applicationId;
    private Long candidateId;
    private Long employerId;
    private Long jobId;
    private Long companyId;

    private String candidateEmail;
    private String candidateName;
    private String jobTitle;
    private String companyName;

    private ApplicationStatus previousStatus;
    private ApplicationStatus newStatus;

    private LocalDateTime changedAt;
}
