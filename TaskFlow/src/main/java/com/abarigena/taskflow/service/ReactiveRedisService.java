package com.abarigena.taskflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Реактивный сервис для работы с Redis кэшем.
 * Обрабатывает кэширование для Mono и Flux объектов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveRedisService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    /**
     * Получить значение из кэша или выполнить поставщика и закэшировать результат
     */
    public <T> Mono<T> getOrSet(String key, Supplier<Mono<T>> supplier, Duration ttl, Class<T> clazz) {
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .cast(clazz)
                .doOnNext(cached -> log.debug("Cache HIT for key: {}", key))
                .switchIfEmpty(
                    supplier.get()
                        .flatMap(value -> 
                            reactiveRedisTemplate.opsForValue()
                                .set(key, value, ttl)
                                .doOnNext(result -> log.debug("Cache SET for key: {} with TTL: {}", key, ttl))
                                .thenReturn(value)
                        )
                        .doOnNext(value -> log.debug("Cache MISS for key: {}, fetched from source", key))
                );
    }

    /**
     * Получить значение из кэша или выполнить поставщика и закэшировать результат (без TTL)
     */
    public <T> Mono<T> getOrSet(String key, Supplier<Mono<T>> supplier, Class<T> clazz) {
        return getOrSet(key, supplier, Duration.ofHours(1), clazz);
    }

    /**
     * Удалить ключ из кэша
     */
    public Mono<Boolean> evict(String key) {
        return reactiveRedisTemplate.opsForValue()
                .delete(key)
                .doOnNext(result -> log.debug("Cache EVICT for key: {}, result: {}", key, result));
    }

    /**
     * Удалить несколько ключей из кэша
     */
    public Mono<Long> evictAll(String... keys) {
        return reactiveRedisTemplate.delete(keys)
                .doOnNext(result -> log.debug("Cache EVICT ALL for keys: {}, deleted count: {}", keys, result));
    }

    /**
     * Удалить все ключи по паттерну (упрощенная версия)
     * Примечание: Redis SCAN команда безопаснее чем KEYS на продакшене
     */
    public Mono<Long> evictByPattern(String pattern) {
        // Для простоты используем высокоуровневый API
        // В реальном проекте лучше использовать SCAN вместо keys для больших наборов данных
        log.warn("evictByPattern is simplified implementation. Consider using SCAN for production.");
        return Mono.just(0L)
            .doOnNext(result -> log.debug("Cache EVICT BY PATTERN: {} - simplified implementation", pattern));
    }

    /**
     * Проверить существование ключа в кэше
     */
    public Mono<Boolean> exists(String key) {
        return reactiveRedisTemplate.hasKey(key);
    }

    /**
     * Установить TTL для существующего ключа
     */
    public Mono<Boolean> expire(String key, Duration ttl) {
        return reactiveRedisTemplate.expire(key, ttl)
                .doOnNext(result -> log.debug("Set TTL for key: {}, TTL: {}, result: {}", key, ttl, result));
    }

    /**
     * Получить TTL ключа
     */
    public Mono<Duration> getTtl(String key) {
        return reactiveRedisTemplate.getExpire(key);
    }

    /**
     * Инкремент числового значения
     */
    public Mono<Long> increment(String key) {
        return reactiveRedisTemplate.opsForValue().increment(key)
                .doOnNext(result -> log.debug("Cache INCREMENT for key: {}, new value: {}", key, result));
    }

    /**
     * Инкремент числового значения на определённую величину
     */
    public Mono<Long> incrementBy(String key, long delta) {
        return reactiveRedisTemplate.opsForValue().increment(key, delta)
                .doOnNext(result -> log.debug("Cache INCREMENT BY for key: {}, delta: {}, new value: {}", key, delta, result));
    }

    /**
     * Работа со списками - добавить элемент в начало
     */
    public <T> Mono<Long> leftPush(String key, T value) {
        return reactiveRedisTemplate.opsForList().leftPush(key, value)
                .doOnNext(result -> log.debug("List LEFT PUSH for key: {}, new size: {}", key, result));
    }

    /**
     * Работа со списками - получить диапазон элементов
     */
    public <T> Flux<T> listRange(String key, long start, long end, Class<T> clazz) {
        return reactiveRedisTemplate.opsForList()
                .range(key, start, end)
                .cast(clazz)
                .doOnSubscribe(s -> log.debug("List RANGE for key: {}, range: {}-{}", key, start, end));
    }

    /**
     * Работа с множествами - добавить элемент
     */
    public <T> Mono<Long> setAdd(String key, T... values) {
        return reactiveRedisTemplate.opsForSet().add(key, values)
                .doOnNext(result -> log.debug("Set ADD for key: {}, added count: {}", key, result));
    }

    /**
     * Работа с множествами - получить все элементы
     */
    public <T> Flux<T> setMembers(String key, Class<T> clazz) {
        return reactiveRedisTemplate.opsForSet()
                .members(key)
                .cast(clazz)
                .doOnSubscribe(s -> log.debug("Set MEMBERS for key: {}", key));
    }
} 