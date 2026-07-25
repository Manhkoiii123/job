package com.manh.job.payload.request;

import com.manh.job.domain.ExperienceLevel;
import com.manh.job.domain.JobStatus;
import com.manh.job.domain.JobType;
import com.manh.job.domain.WorkMode;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JobSearchRequest {
    private  String keyword;

    private Long categoryId;

    private List<Long> skillIds;
    private List<Long> tagIds;

    private Long companyId;

    private String location;

    private BigDecimal minSalary;
    private BigDecimal maxSalary;

    private JobType jobType;

    private WorkMode workMode;

    private ExperienceLevel  experienceLevel;

    private JobStatus status;

    private Integer minOpenings;
    private Integer maxOpenings;
}
