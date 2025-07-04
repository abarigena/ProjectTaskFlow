package com.abarigena.taskflow.service;

import com.abarigena.taskflow.dto.CacheInvalidationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;

/**
 * Сервис для подписки на события через Redis Pub/Sub и инвалидации кэша
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisEventSubscriber {
    
    private final ReactiveRedisConnectionFactory connectionFactory;
    private final ReactiveRedisService reactiveRedisService;
    private final ObjectMapper redisObjectMapper;
    
    private ReactiveRedisMessageListenerContainer container;
    
    // Константы для названий каналов (те же что и в Publisher)
    private static final String CHANNEL_PREFIX = "taskflow:";
    private static final String USER_UPDATED_CHANNEL = CHANNEL_PREFIX + "user:updated";
    private static final String USER_DELETED_CHANNEL = CHANNEL_PREFIX + "user:deleted";
    private static final String PROJECT_UPDATED_CHANNEL = CHANNEL_PREFIX + "project:updated";
    private static final String PROJECT_DELETED_CHANNEL = CHANNEL_PREFIX + "project:deleted";
    private static final String TASK_UPDATED_CHANNEL = CHANNEL_PREFIX + "task:updated";
    private static final String TASK_DELETED_CHANNEL = CHANNEL_PREFIX + "task:deleted";
    
    @PostConstruct
    public void initialize() {
        // Создаем контейнер для прослушивания Redis сообщений
        container = new ReactiveRedisMessageListenerContainer(connectionFactory);
        
        // Подписываемся на все каналы
        subscribeToUserEvents();
        subscribeToProjectEvents();
        subscribeToTaskEvents();
        
        log.info("Redis Pub/Sub subscriber initialized and listening to channels");
    }
    
    @PreDestroy
    public void destroy() {
        if (container != null) {
            try {
                container.destroyLater()
                    .doOnSuccess(v -> log.info("Redis Pub/Sub subscriber destroyed"))
                    .doOnError(error -> log.warn("Error during Redis container destruction: {}", error.getMessage()))
                    .onErrorComplete()  // Игнорируем ошибки при закрытии
                    .subscribe();
            } catch (Exception e) {
                log.warn("Error during Redis container destruction: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Подписка на события пользователей
     */
    private void subscribeToUserEvents() {
        receiveAndDeserialize(USER_UPDATED_CHANNEL, CacheInvalidationEvent.class)
                .flatMap(this::handleUserUpdated)
                .doOnError(error -> log.error("Error processing user updated event", error))
                .subscribe();

        receiveAndDeserialize(USER_DELETED_CHANNEL, CacheInvalidationEvent.class)
                .flatMap(this::handleUserDeleted)
                .doOnError(error -> log.error("Error processing user deleted event", error))
                .subscribe();
    }
    
    /**
     * Подписка на события проектов
     */
    private void subscribeToProjectEvents() {
        receiveAndDeserialize(PROJECT_UPDATED_CHANNEL, CacheInvalidationEvent.class)
                .flatMap(this::handleProjectUpdated)
                .doOnError(error -> log.error("Error processing project updated event", error))
                .subscribe();

        receiveAndDeserialize(PROJECT_DELETED_CHANNEL, CacheInvalidationEvent.class)
                .flatMap(this::handleProjectDeleted)
                .doOnError(error -> log.error("Error processing project deleted event", error))
                .subscribe();
    }
    
    /**
     * Подписка на события задач
     */
    private void subscribeToTaskEvents() {
        receiveAndDeserialize(TASK_UPDATED_CHANNEL, CacheInvalidationEvent.class)
                .flatMap(this::handleTaskUpdated)
                .doOnError(error -> log.error("Error processing task updated event", error))
                .subscribe();

        receiveAndDeserialize(TASK_DELETED_CHANNEL, CacheInvalidationEvent.class)
                .flatMap(this::handleTaskDeleted)
                .doOnError(error -> log.error("Error processing task deleted event", error))
                .subscribe();
    }

    /**
     * Хелпер для подписки на канал и десериализации сообщения
     */
    private <T> Flux<T> receiveAndDeserialize(String channel, Class<T> clazz) {
        return container.receive(new ChannelTopic(channel))
                .map(ReactiveSubscription.Message::getMessage)
                .flatMap(json -> {
                    try {
                        return Mono.just(redisObjectMapper.readValue(json, clazz));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse event from channel {}", channel, e);
                        return Mono.empty();
                    }
                });
    }
    
    /**
     * Обработка события обновления пользователя
     */
    private Mono<Void> handleUserUpdated(CacheInvalidationEvent event) {
        log.info("Received user updated event: {}", event);
        
        Long userId = event.getEntityId();
        Map<String, Object> metadata = event.getMetadata();
        
        // Инвалидируем кэш по ID
        String userIdKey = "user:id:" + userId;
        
        // Инвалидируем кэш по email (если есть информация о старом email)
        Mono<Void> evictEmailCache = Mono.empty();
        if (metadata != null && metadata.containsKey("oldEmail")) {
            String oldEmailKey = "user:email:" + metadata.get("oldEmail");
            evictEmailCache = reactiveRedisService.evict(oldEmailKey).then();
        }
        if (metadata != null && metadata.containsKey("newEmail")) {
            String newEmailKey = "user:email:" + metadata.get("newEmail");
            evictEmailCache = evictEmailCache.then(reactiveRedisService.evict(newEmailKey)).then();
        }
        
        return reactiveRedisService.evict(userIdKey)
            .then(evictEmailCache)
            .doOnSuccess(v -> log.info("Cache invalidated for user ID: {}", userId));
    }
    
    /**
     * Обработка события удаления пользователя
     */
    private Mono<Void> handleUserDeleted(CacheInvalidationEvent event) {
        log.info("Received user deleted event: {}", event);
        
        Long userId = event.getEntityId();
        Map<String, Object> metadata = event.getMetadata();
        
        String userIdKey = "user:id:" + userId;
        
        // Инвалидируем кэш по email (если есть информация)
        Mono<Void> evictEmailCache = Mono.empty();
        if (metadata != null && metadata.containsKey("email")) {
            String emailKey = "user:email:" + metadata.get("email");
            evictEmailCache = reactiveRedisService.evict(emailKey).then();
        }
        
        return reactiveRedisService.evict(userIdKey)
            .then(evictEmailCache)
            .doOnSuccess(v -> log.info("Cache invalidated for deleted user ID: {}", userId));
    }
    
    /**
     * Обработка события обновления проекта
     */
    private Mono<Void> handleProjectUpdated(CacheInvalidationEvent event) {
        log.info("Received project updated event: {}", event);
        
        Long projectId = event.getEntityId();
        String projectKey = "project:id:" + projectId;
        
        return reactiveRedisService.evict(projectKey)
            .doOnSuccess(v -> log.info("Cache invalidated for project ID: {}", projectId))
            .then();
    }
    
    /**
     * Обработка события удаления проекта
     */
    private Mono<Void> handleProjectDeleted(CacheInvalidationEvent event) {
        log.info("Received project deleted event: {}", event);
        
        Long projectId = event.getEntityId();
        String projectKey = "project:id:" + projectId;
        
        return reactiveRedisService.evict(projectKey)
            .doOnSuccess(v -> log.info("Cache invalidated for deleted project ID: {}", projectId))
            .then();
    }
    
    /**
     * Обработка события обновления задачи
     */
    private Mono<Void> handleTaskUpdated(CacheInvalidationEvent event) {
        log.info("Received task updated event: {}", event);
        
        Long taskId = event.getEntityId();
        String taskKey = "task:id:" + taskId;
        
        return reactiveRedisService.evict(taskKey)
            .doOnSuccess(v -> log.info("Cache invalidated for task ID: {}", taskId))
            .then();
    }
    
    /**
     * Обработка события удаления задачи
     */
    private Mono<Void> handleTaskDeleted(CacheInvalidationEvent event) {
        log.info("Received task deleted event: {}", event);
        
        Long taskId = event.getEntityId();
        String taskKey = "task:id:" + taskId;
        
        return reactiveRedisService.evict(taskKey)
            .doOnSuccess(v -> log.info("Cache invalidated for deleted task ID: {}", taskId))
            .then();
    }
} 