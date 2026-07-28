package com.manh.job.repository;

import com.manh.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobRepository extends JpaRepository<Job,Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByCompanyId(Long companyId);
}
