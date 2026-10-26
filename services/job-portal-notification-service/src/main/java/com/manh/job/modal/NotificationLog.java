package com.manh.job.modal;

import com.manh.job.domain.ApplicationStatus;
import com.manh.job.domain.NotificationDeliveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;
    private Long candidateId;
    private String recipientEmail;
    private String subject;

    @Column(length = 4000)
    private String bodyPreview;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationDeliveryStatus deliveryStatus;

    @Column(length = 2000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
