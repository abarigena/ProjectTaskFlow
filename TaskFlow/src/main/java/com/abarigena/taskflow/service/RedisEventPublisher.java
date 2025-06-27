package com.abarigena.taskflow.service;

import com.abarigena.taskflow.dto.CacheInvalidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Сервис для публикации событий через Redis Pub/Sub
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {
    
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    
    // Константы для названий каналов
    private static final String CHANNEL_PREFIX = "taskflow:";
    private static final String USER_UPDATED_CHANNEL = CHANNEL_PREFIX + "user:updated";
    private static final String USER_DELETED_CHANNEL = CHANNEL_PREFIX + "user:deleted";
    private static final String PROJECT_UPDATED_CHANNEL = CHANNEL_PREFIX + "project:updated";
    private static final String PROJECT_DELETED_CHANNEL = CHANNEL_PREFIX + "project:deleted";
    private static final String TASK_UPDATED_CHANNEL = CHANNEL_PREFIX + "task:updated";
    private static final String TASK_DELETED_CHANNEL = CHANNEL_PREFIX + "task:deleted";
    
    /**
     * Публикует событие обновления пользователя
     */
    public Mono<Void> publishUserUpdated(Long userId, Map<String, Object> metadata) {
        return publishEvent(
            USER_UPDATED_CHANNEL,
            CacheInvalidationEvent.EventType.USER_UPDATED,
            userId,
            metadata
        );
    }
    
    /**
     * Публикует событие удаления пользователя
     */
    public Mono<Void> publishUserDeleted(Long userId, Map<String, Object> metadata) {
        return publishEvent(
            USER_DELETED_CHANNEL,
            CacheInvalidationEvent.EventType.USER_DELETED,
            userId,
            metadata
        );
    }
    
    /**
     * Публикует событие обновления проекта
     */
    public Mono<Void> publishProjectUpdated(Long projectId) {
        return publishEvent(
            PROJECT_UPDATED_CHANNEL,
            CacheInvalidationEvent.EventType.PROJECT_UPDATED,
            projectId,
            null
        );
    }
    
    /**
     * Публикует событие удаления проекта
     */
    public Mono<Void> publishProjectDeleted(Long projectId) {
        return publishEvent(
            PROJECT_DELETED_CHANNEL,
            CacheInvalidationEvent.EventType.PROJECT_DELETED,
            projectId,
            null
        );
    }
    
    /**
     * Публикует событие обновления задачи
     */
    public Mono<Void> publishTaskUpdated(Long taskId) {
        return publishEvent(
            TASK_UPDATED_CHANNEL,
            CacheInvalidationEvent.EventType.TASK_UPDATED,
            taskId,
            null
        );
    }
    
    /**
     * Публикует событие удаления задачи
     */
    public Mono<Void> publishTaskDeleted(Long taskId) {
        return publishEvent(
            TASK_DELETED_CHANNEL,
            CacheInvalidationEvent.EventType.TASK_DELETED,
            taskId,
            null
        );
    }
    
    /**
     * Основной метод для публикации события
     */
    private Mono<Void> publishEvent(String channel, CacheInvalidationEvent.EventType eventType, 
                                   Long entityId, Map<String, Object> metadata) {
        
        CacheInvalidationEvent event = CacheInvalidationEvent.builder()
            .eventType(eventType)
            .entityId(entityId)
            .metadata(metadata)
            .timestamp(LocalDateTime.now())
            .source("TaskFlow")
            .build();
        
        return reactiveRedisTemplate.convertAndSend(channel, event)
            .doOnSuccess(count -> 
                log.info("Published {} event for entity ID {} to channel '{}'. Subscribers notified: {}", 
                    eventType, entityId, channel, count))
            .doOnError(error -> 
                log.error("Failed to publish {} event for entity ID {} to channel '{}'", 
                    eventType, entityId, channel, error))
            .then();
    }
} 