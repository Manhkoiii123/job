package com.manh.job.event;

import com.manh.job.dto.event.ApplicationStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStatusChangedPublisher {

    private final KafkaTemplate<String, ApplicationStatusChangedEvent> kafkaTemplate;

    @Value("${job.kafka.topics.application-status-changed:application.status.changed}")
    private String topic;

    public void publish(ApplicationStatusChangedEvent event) {
        String key = String.valueOf(event.getApplicationId());
        kafkaTemplate.send(topic, key, event);
        log.info("Published ApplicationStatusChangedEvent applicationId={} {} -> {} to topic={}",
                event.getApplicationId(), event.getPreviousStatus(), event.getNewStatus(), topic);
    }
}
