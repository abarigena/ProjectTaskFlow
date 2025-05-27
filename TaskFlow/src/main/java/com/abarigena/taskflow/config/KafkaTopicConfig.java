package com.abarigena.taskflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Конфигурация для создания топиков Kafka.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.taskflow-events}")
    private String taskflowEventsTopic;

    @Value("${app.kafka.topics.taskflow-dlq}")
    private String taskflowDlqTopic;

    @Bean
    public NewTopic taskflowEventsTopic() {
        return TopicBuilder.name(taskflowEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskflowDlqTopic() {
        return TopicBuilder.name(taskflowDlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}