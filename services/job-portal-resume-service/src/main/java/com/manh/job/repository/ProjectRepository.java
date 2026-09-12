package com.manh.job.repository;

import com.manh.job.modal.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByResume_IdOrderByDisplayOrderAsc(Long resumeId);
}
