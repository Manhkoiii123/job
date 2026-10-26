package com.manh.job.repository;

import com.manh.job.modal.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
