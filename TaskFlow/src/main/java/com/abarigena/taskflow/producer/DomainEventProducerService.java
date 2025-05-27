package com.abarigena.taskflow.producer;


import com.abarigena.taskflow.dto.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис для отправки доменных событий в Apache Kafka.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DomainEventProducerService {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @Value("${app.kafka.topics.taskflow-events}")
    private String taskflowEventsTopic;

    /**
     * Отправляет доменное событие в Kafka.
     * @param event Доменное событие для отправки.
     */
    public void sendDomainEvent(DomainEvent event) {
        if (event == null) {
            log.warn("Попытка отправить null событие. Операция прервана.");
            return;
        }
        log.info("Отправка доменного события в топик '{}': {}", taskflowEventsTopic, event);


        CompletableFuture<SendResult<String, DomainEvent>> future = kafkaTemplate.send(taskflowEventsTopic, event.getEntityId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Доменное событие успешно отправлено: partition={}, offset={}, value={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        result.getProducerRecord().value());
            } else {
                log.error("Ошибка при отправке доменного события: {}, событие: {}",
                        ex.getMessage(),
                        event,
                        ex);
            }
        });
    }
}