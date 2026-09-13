package com.manh.job.modal;

import com.manh.job.domain.ExperienceLevel;
import com.manh.job.domain.JobStatus;
import com.manh.job.domain.JobType;
import com.manh.job.domain.WorkMode;
import com.manh.job.modal.embeddable.JobLocation;
import com.manh.job.modal.embeddable.SalaryRange;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name="jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String requirements;

    private String responsibilities;

    private String benefits;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long employerId;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "category_id", nullable = false)
        private JobCategory category;

        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(
            name = "job_job_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
        )
        @Builder.Default
        private Set<JobSkill> skills = new HashSet<>();

        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(
            name = "job_job_tags",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
        )
        @Builder.Default
        private Set<JobTag> tags = new HashSet<>();

    @Embedded
    private JobLocation location;

    @Embedded
    private SalaryRange salaryRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType jobType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private Integer openings = 1;

    private LocalDate applicationDeadline;

    private  LocalDate expiresAt;

    private  Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime closedAt;



}
