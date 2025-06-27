package com.abarigena.taskflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для событий инвалидации кэша через Redis Pub/Sub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationEvent {
    
    /**
     * Тип события (USER_UPDATED, PROJECT_DELETED, etc.)
     */
    private EventType eventType;
    
    /**
     * ID сущности, которая была изменена
     */
    private Long entityId;
    
    /**
     * Дополнительная информация о событии (например, старый email пользователя)
     */
    private Map<String, Object> metadata;
    
    /**
     * Время события
     */
    private LocalDateTime timestamp;
    
    /**
     * Источник события (имя сервиса)
     */
    private String source;
    
    /**
     * Типы событий для инвалидации кэша
     */
    public enum EventType {
        USER_UPDATED,
        USER_DELETED,
        PROJECT_UPDATED,
        PROJECT_DELETED,
        TASK_UPDATED,
        TASK_DELETED
    }
} 