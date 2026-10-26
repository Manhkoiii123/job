package com.manh.job.service;

import com.manh.job.dto.event.ApplicationStatusChangedEvent;

public interface EmailNotificationService {
    void handleStatusChanged(ApplicationStatusChangedEvent event);
}
