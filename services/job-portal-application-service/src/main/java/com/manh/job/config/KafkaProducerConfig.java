package com.manh.job.config;

import com.manh.job.dto.event.ApplicationStatusChangedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${job.kafka.topics.application-status-changed:application.status.changed}")
    private String applicationStatusChangedTopic;

    @Bean
    public ProducerFactory<String, ApplicationStatusChangedEvent> applicationStatusProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, ApplicationStatusChangedEvent> applicationStatusKafkaTemplate(
            ProducerFactory<String, ApplicationStatusChangedEvent> applicationStatusProducerFactory
    ) {
        return new KafkaTemplate<>(applicationStatusProducerFactory);
    }

    @Bean
    public NewTopic applicationStatusChangedTopic() {
        return TopicBuilder.name(applicationStatusChangedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
