package com.manh.job.kafka;

import com.manh.job.dto.event.ApplicationStatusChangedEvent;
import com.manh.job.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventConsumer {

    private final EmailNotificationService emailNotificationService;

    @KafkaListener(
            topics = "${job.kafka.topics.application-status-changed:application.status.changed}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}",
            containerFactory = "applicationStatusKafkaListenerContainerFactory"
    )
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        log.info("Consumed ApplicationStatusChangedEvent applicationId={} {} -> {}",
                event.getApplicationId(), event.getPreviousStatus(), event.getNewStatus());
        emailNotificationService.handleStatusChanged(event);
    }
}
