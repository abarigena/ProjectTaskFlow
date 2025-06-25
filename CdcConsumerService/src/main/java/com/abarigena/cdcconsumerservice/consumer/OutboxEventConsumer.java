package com.abarigena.cdcconsumerservice.consumer;

import com.abarigena.cdcconsumerservice.dto.DebeziumEvent;
import com.abarigena.cdcconsumerservice.dto.OutboxEventData;
import com.abarigena.cdcconsumerservice.service.OpenSearchSyncService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventConsumer {

    private final ObjectMapper objectMapper;
    private final OpenSearchSyncService openSearchSyncService;

    /**
     * Слушаем CDC события из outbox таблицы.
     * Здесь реализуется паттерн Outbox - мы обрабатываем события из таблицы-посредника.
     */
    @KafkaListener(
        topics = "taskflow.public.outbox_events",
        groupId = "cdc-consumer-group"
    )
    public void handleOutboxEvent(@Payload String message) {
        log.info("🔥 Получено CDC событие из outbox: {}", message);
        
        try {
            // Парсим Debezium событие
            DebeziumEvent<OutboxEventData> debeziumEvent = 
                objectMapper.readValue(message, objectMapper.getTypeFactory()
                    .constructParametricType(DebeziumEvent.class, OutboxEventData.class));
            
            // Обрабатываем только операции создания (INSERT) и обновления (UPDATE)
            if ("c".equals(debeziumEvent.getOperation()) || "u".equals(debeziumEvent.getOperation())) {
                OutboxEventData eventData = debeziumEvent.getAfter();
                
                if (eventData != null) {
                    processBusinessEvent(eventData);
                    // Синхронизируем с OpenSearch
                    syncToOpenSearch(eventData);
                }
            } else if ("d".equals(debeziumEvent.getOperation())) {
                log.info("🗑️ Событие удаления outbox записи: {}", debeziumEvent.getBefore());
            }
            
        } catch (JsonProcessingException e) {
            log.error("❌ Ошибка парсинга CDC события: {}", e.getMessage(), e);
        }
    }

    /**
     * Обрабатываем бизнес-логику основываясь на типе события из outbox
     */
    private void processBusinessEvent(OutboxEventData eventData) {
        log.info("📋 Обрабатываем бизнес-событие:");
        log.info("   🆔 ID: {}", eventData.getId());
        log.info("   📝 Тип: {}", eventData.getEventType());
        log.info("   📄 Данные: {}", eventData.getEventData());
        log.info("   🕒 Создано: {}", eventData.getCreatedAt());
        
        // Здесь мы можем маршрутизировать события по типам
        switch (eventData.getEventType()) {
            case "USER_CREATED":
                handleUserCreated(eventData.getEventData());
                break;
            case "USER_UPDATED":
                handleUserUpdated(eventData.getEventData());
                break;
            case "USER_PROFILE_VIEWED":
                handleUserProfileViewed(eventData.getEventData());
                break;
            case "USER_LOGIN":
                handleUserLogin(eventData.getEventData());
                break;
            case "ORDER_CREATED":
                handleOrderCreated(eventData.getEventData());
                break;
            case "NOTIFICATION_SENT":
                handleNotificationSent(eventData.getEventData());
                break;
            default:
                log.warn("⚠️ Неизвестный тип события: {}", eventData.getEventType());
        }
    }

    private void handleUserCreated(String eventData) {
        log.info("🎉 Обработка создания пользователя: {}", eventData);
        // Индексируем пользователя в OpenSearch для поиска
        openSearchSyncService.indexUser(eventData);
    }

    private void handleUserUpdated(String eventData) {
        log.info("🔄 Обработка обновления пользователя: {}", eventData);
        // Обновляем данные в OpenSearch
        try {
            JsonNode userNode = objectMapper.readTree(eventData);
            if (userNode.has("userId")) {
                openSearchSyncService.indexUser(eventData);
            }
        } catch (Exception e) {
            log.error("Ошибка обновления пользователя в OpenSearch: {}", e.getMessage());
        }
    }

    private void handleUserProfileViewed(String eventData) {
        log.info("👁️ Обработка просмотра профиля: {}", eventData);
        // Сохраняем аналитику в OpenSearch
    }

    private void handleUserLogin(String eventData) {
        log.info("🔐 Обработка входа пользователя: {}", eventData);
        // Логика для отслеживания входов, безопасности, аналитики
    }

    private void handleOrderCreated(String eventData) {
        log.info("🛒 Обработка создания заказа: {}", eventData);
        // Логика для обработки заказов, отправки уведомлений и т.д.
    }

    private void handleNotificationSent(String eventData) {
        log.info("📧 Обработка отправленного уведомления: {}", eventData);
        // Логика для отслеживания статуса уведомлений
    }

    /**
     * Синхронизирует все события с OpenSearch для аналитики
     */
    private void syncToOpenSearch(OutboxEventData eventData) {
        try {
            JsonNode dataNode = objectMapper.readTree(eventData.getEventData());
            Long userId = dataNode.has("userId") ? dataNode.get("userId").asLong() : null;
            
            if (userId != null) {
                openSearchSyncService.syncUserEvent(userId, eventData.getEventType(), eventData.getEventData());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка синхронизации события с OpenSearch: {}", e.getMessage());
        }
    }
} 