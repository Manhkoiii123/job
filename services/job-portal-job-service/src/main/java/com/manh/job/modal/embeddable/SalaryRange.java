package com.manh.job.modal.embeddable;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryRange {
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
}
