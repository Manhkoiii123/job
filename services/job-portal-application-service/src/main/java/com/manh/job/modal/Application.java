package com.manh.job.modal;

import com.manh.job.domain.AiShortListStatus;
import com.manh.job.domain.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name="applications")
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Long candidateId;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long employerId;

    @Column(nullable = false)
    private Long resumeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    private String coverLetter;

    private BigDecimal expectedSalary;

    private LocalDate availableFrom;

    private Boolean isStarred = false;

    @Column
    private Integer aiScore;

    private AiShortListStatus aiShortListStatus = AiShortListStatus.PENDING_REVIEW;

    private LocalDateTime withdrawnAt;

    private String withdrawnReason;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime appliedAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}

