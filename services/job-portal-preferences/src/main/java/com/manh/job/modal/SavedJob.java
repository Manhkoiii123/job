package com.manh.job.modal;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "saved_jobs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saved_jobs_candidate_job",
                columnNames = {"candidate_id", "job_id"}
        )
)
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    /** Denormalized from Job/Company services for local filter without Kafka sync yet. */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime savedAt;
}
