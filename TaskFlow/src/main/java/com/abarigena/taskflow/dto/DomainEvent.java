package com.abarigena.taskflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для представления доменного события, отправляемого в Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {
    /**
     * Тип события, например, TASK_CREATED.
     */
    private String eventType;
    /**
     * Идентификатор сущности (задачи, проекта, комментария).
     * Может быть null, если событие не связано с конкретной сущностью.
     */
    private String entityId;
    /**
     * Тип сущности (TASK, PROJECT, COMMENT).
     * Может быть null, если событие не связано с конкретной сущностью.
     */
    private String entityType;
    /**
     * Полезная нагрузка события, содержащая детали сущности.
     */
    private Object payload;
    /**
     * Время создания события.
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}