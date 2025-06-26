package com.abarigena.cdcconsumerservice.consumer;

import com.abarigena.cdcconsumerservice.dto.DebeziumEvent;
import com.abarigena.cdcconsumerservice.service.OpenSearchSyncService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumer для обработки CDC событий от реальных бизнес-таблиц
 * Заменяет OutboxEventConsumer для продакшн использования
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessEntityConsumer {

    private final ObjectMapper objectMapper;
    private final OpenSearchSyncService openSearchSyncService;

    /**
     * Обрабатываем CDC события от таблицы tasks
     */
    @KafkaListener(
        topics = "tasks",
        groupId = "cdc-business-group"
    )
    public void handleTaskEvent(@Payload String message) {
        log.info("🎯 Получено CDC событие от таблицы tasks: {}", message);
        handleEntityEvent("tasks", message);
    }

    /**
     * Обрабатываем CDC события от таблицы comments
     */
    @KafkaListener(
        topics = "comments",
        groupId = "cdc-business-group"
    )
    public void handleCommentEvent(@Payload String message) {
        log.info("💬 Получено CDC событие от таблицы comments: {}", message);
        handleEntityEvent("comments", message);
    }

    /**
     * Обрабатываем CDC события от таблицы projects
     */
    @KafkaListener(
        topics = "projects",
        groupId = "cdc-business-group"
    )
    public void handleProjectEvent(@Payload String message) {
        log.info("📁 Получено CDC событие от таблицы projects: {}", message);
        handleEntityEvent("projects", message);
    }

    /**
     * Обрабатываем CDC события от таблицы users
     */
    @KafkaListener(
        topics = "users",
        groupId = "cdc-business-group"
    )
    public void handleUserEvent(@Payload String message) {
        log.info("👤 Получено CDC событие от таблицы users: {}", message);
        handleEntityEvent("users", message);
    }

    /**
     * Универсальная обработка CDC событий
     */
    private void handleEntityEvent(String entityType, String message) {
        try {
            // Парсим Debezium событие как Map для универсальности
            TypeReference<DebeziumEvent<Map<String, Object>>> typeRef = 
                new TypeReference<DebeziumEvent<Map<String, Object>>>() {};
            DebeziumEvent<Map<String, Object>> debeziumEvent = 
                objectMapper.readValue(message, typeRef);
            
            String operation = debeziumEvent.getOperation();
            log.info("📋 Операция: {} для сущности: {}", operation, entityType);
            
            switch (operation) {
                case "c": // CREATE
                case "u": // UPDATE
                case "r": // READ (Initial snapshot)
                    if (debeziumEvent.getAfter() != null) {
                        // Используем типизированный метод для лучшей обработки данных
                        openSearchSyncService.indexEntityTyped(entityType, debeziumEvent.getAfter());
                        String operationName = switch (operation) {
                            case "c" -> "создана";
                            case "u" -> "обновлена";
                            case "r" -> "загружена из snapshot";
                            default -> "обработана";
                        };
                        log.info("✅ Сущность {} {} и проиндексирована в OpenSearch", entityType, operationName);
                    }
                    break;
                case "d": // DELETE
                    if (debeziumEvent.getBefore() != null) {
                        Map<String, Object> beforeData = debeziumEvent.getBefore();
                        Object id = beforeData.get("id");
                        if (id != null) {
                            openSearchSyncService.deleteEntity(entityType, String.valueOf(id));
                            log.info("🗑️ Сущность {} с ID {} удалена из OpenSearch", entityType, id);
                        }
                    }
                    break;
                default:
                    log.warn("⚠️ Неизвестная операция CDC: {}", operation);
            }
            
        } catch (JsonProcessingException e) {
            log.error("❌ Ошибка парсинга CDC события для {}: {}", entityType, e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Ошибка обработки CDC события для {}: {}", entityType, e.getMessage(), e);
        }
    }
} 