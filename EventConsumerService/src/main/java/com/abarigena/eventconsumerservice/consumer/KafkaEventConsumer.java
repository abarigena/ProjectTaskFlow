package com.abarigena.eventconsumerservice.consumer;

import com.abarigena.eventconsumerservice.dto.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventConsumer {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> dlqKafkaTemplate;

    @Value("${app.kafka.topics.taskflow-dlq}")
    private String dlqTopic;

    private static final String TASKFLOW_EVENTS_TOPIC = "taskflow.events";

    private final List<DomainEvent> processedEvents = new CopyOnWriteArrayList<>();
    private final List<DomainEvent> dlqEvents = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = TASKFLOW_EVENTS_TOPIC)
    public void listenToTaskflowEvents(
            @Payload DomainEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Получено событие: [Топик: {}, Раздел: {}, Смещение: {}]", topic, partition, offset);
        final DomainEvent currentEvent = event;

        try {
            if ("FORCE_DLQ_TEST".equals(currentEvent.getEventType())) {
                throw new RuntimeException("Принудительная ошибка для тестирования DLQ.");
            }

            String eventPayloadJson = objectMapper.writeValueAsString(currentEvent.getPayload());
            log.info("Тип события: {}, ID сущности: {}, Тип сущности: {}, Время создания: {}, Полезная нагрузка: {}",
                    currentEvent.getEventType(), currentEvent.getEntityId(), currentEvent.getEntityType(), currentEvent.getCreatedAt(), eventPayloadJson);

            switch (currentEvent.getEntityType()) {
                case "TASK":
                    log.info("[Обработка] Событие ЗАДАЧИ для ID: {}. Детали в общей полезной нагрузке.", currentEvent.getEntityId());
                    break;
                case "PROJECT":
                    log.info("[Обработка] Событие ПРОЕКТА для ID: {}. Детали в общей полезной нагрузке.", currentEvent.getEntityId());
                    break;
                case "COMMENT":
                    log.info("[Обработка] Событие КОММЕНТАРИЯ для ID: {}. Детали в общей полезной нагрузке.", currentEvent.getEntityId());
                    break;
                default:
                    log.warn("Получен неизвестный тип сущности: {}", currentEvent.getEntityType());
                    break;
            }

            processedEvents.add(currentEvent);
            log.info("Событие {} ID {} добавлено в список обработанных.", currentEvent.getEventType(), currentEvent.getEntityId());
            acknowledgment.acknowledge();
            log.info("Событие подтверждено для ID сущности: {}", currentEvent.getEntityId());

        } catch (Exception e) {
            log.error("Ошибка обработки события: Тип {} ID {}. Сообщение: {}. Отправка в DLQ.",
                    currentEvent.getEventType(), currentEvent.getEntityId(), e.getMessage(), e);
            dlqEvents.add(currentEvent);
            log.info("Событие {} ID {} добавлено в список DLQ перед отправкой.", currentEvent.getEventType(), currentEvent.getEntityId());
            sendToDlqAndAck(currentEvent, acknowledgment);
        }
    }

    private void sendToDlqAndAck(DomainEvent event, Acknowledgment acknowledgment) {
        try {
            dlqKafkaTemplate.send(dlqTopic, event.getEntityId(), event);
            log.info("Событие успешно отправлено в DLQ топик: {} для ID сущности: {}", dlqTopic, event.getEntityId());
            acknowledgment.acknowledge();
            log.info("Исходное событие подтверждено после отправки в DLQ.");
        } catch (Exception dlqEx) {
            log.error("КРИТИЧЕСКАЯ ОШИБКА: Не удалось отправить событие в DLQ топик {}: {} (ID события: {})",
                    dlqTopic, dlqEx.getMessage(), event.getEntityId(), dlqEx);
        }
    }

    public List<DomainEvent> getProcessedEvents() {
        return Collections.unmodifiableList(processedEvents);
    }

    public List<DomainEvent> getDlqEvents() {
        return Collections.unmodifiableList(dlqEvents);
    }

    public void clearProcessedEvents() { this.processedEvents.clear(); }
    public void clearDlqEvents() { this.dlqEvents.clear(); }
}
