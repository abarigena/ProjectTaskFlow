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

    /**
     * Прослушивает Kafka топик {@code taskflow.events}, обрабатывает входящие доменные события.
     * В случае ошибки при обработке, отправляет событие в Dead Letter Queue (DLQ).
     *
     * @param event полученное доменное событие
     * @param topic топик Kafka, из которого получено событие
     * @param partition раздел Kafka, из которого получено событие
     * @param offset смещение сообщения в разделе Kafka
     * @param acknowledgment объект для подтверждения обработки сообщения
     */
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

            String entityTypeUpper = currentEvent.getEntityType() != null ? currentEvent.getEntityType().toUpperCase() : null;

            switch (entityTypeUpper) {
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

    /**
     * Возвращает неизменяемый список успешно обработанных доменных событий.
     * Предназначен для использования в контроллерах или для тестирования.
     * @return список обработанных событий
     */
    public List<DomainEvent> getProcessedEvents() {
        return Collections.unmodifiableList(processedEvents);
    }

    /**
     * Возвращает неизменяемый список доменных событий, которые не удалось обработать и которые были отправлены в DLQ.
     * Предназначен для использования в контроллерах или для тестирования.
     * @return список событий из DLQ
     */
    public List<DomainEvent> getDlqEvents() {
        return Collections.unmodifiableList(dlqEvents);
    }

    /**
     * Очищает список успешно обработанных событий.
     * Используется в основном для тестовых целей или сброса состояния.
     */
    public void clearProcessedEvents() { this.processedEvents.clear(); }

    /**
     * Очищает список событий, отправленных в DLQ.
     * Используется в основном для тестовых целей или сброса состояния.
     */
    public void clearDlqEvents() { this.dlqEvents.clear(); }
}
