package com.abarigena.taskflow.storeNoSQL.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность для хранения логов событий в MongoDB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "event_logs")
public class EventLog {

    @Id
    private String id;

    private String eventType; // Тип события (TASK_CREATED, COMMENT_ADDED и т.д.)
    private String entityId; // Идентификатор сущности, связанной с событием
    private String entityType; // Тип сущности (TASK, PROJECT, COMMENT)
    private String payload; // Детали события (в формате JSON)
    private LocalDateTime createdAt; // Время создания события

    public static class EventLogBuilder {
        private String id = UUID.randomUUID().toString();
        private LocalDateTime createdAt = LocalDateTime.now();
    }

}
