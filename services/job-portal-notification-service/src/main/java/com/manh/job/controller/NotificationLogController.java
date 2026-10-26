package com.manh.job.controller;

import com.manh.job.modal.NotificationLog;
import com.manh.job.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationLogController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping("/logs/application/{applicationId}")
    public ResponseEntity<List<NotificationLog>> byApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(
                notificationLogRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
        );
    }

    @GetMapping("/logs")
    public ResponseEntity<List<NotificationLog>> all() {
        return ResponseEntity.ok(notificationLogRepository.findAll());
    }
}
