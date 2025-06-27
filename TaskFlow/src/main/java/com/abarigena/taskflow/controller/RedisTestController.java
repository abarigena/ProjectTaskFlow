package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.service.ReactiveRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/redis-test")
@RequiredArgsConstructor
@Slf4j
public class RedisTestController {

    private final ReactiveRedisService reactiveRedisService;

    /**
     * Простой тест записи и чтения строки
     */
    @GetMapping("/simple-test")
    public Mono<Map<String, Object>> simpleTest() {
        String key = "test:simple:" + System.currentTimeMillis();
        String value = "Hello Redis! Current time: " + LocalDateTime.now();
        
        return reactiveRedisService.getOrSet(
                key,
                () -> Mono.just(value),
                Duration.ofMinutes(5),
                String.class
            )
            .map(result -> Map.of(
                "key", key,
                "value", result,
                "timestamp", LocalDateTime.now(),
                "message", "Redis connection successful!"
            ));
    }

    /**
     * Тест кэширования объекта
     */
    @GetMapping("/object-test")
    public Mono<Map<String, Object>> objectTest() {
        String key = "test:object:" + System.currentTimeMillis();
        
        TestObject testObject = new TestObject(
            "Test Name",
            42,
            LocalDateTime.now(),
            true
        );
        
        return reactiveRedisService.getOrSet(
                key,
                () -> Mono.just(testObject),
                Duration.ofMinutes(5),
                TestObject.class
            )
            .map(result -> Map.of(
                "key", key,
                "cached_object", result,
                "message", "Object caching successful!"
            ));
    }

    /**
     * Тест инкремента счётчика
     */
    @PostMapping("/counter-test")
    public Mono<Map<String, Object>> counterTest() {
        String key = "test:counter";
        
        return reactiveRedisService.increment(key)
            .map(count -> Map.of(
                "key", key,
                "count", count,
                "message", "Counter incremented successfully!"
            ));
    }

    /**
     * Тест работы со списком
     */
    @PostMapping("/list-test")
    public Mono<Map<String, Object>> listTest(@RequestParam String item) {
        String key = "test:list";
        
        return reactiveRedisService.leftPush(key, "Item: " + item + " at " + LocalDateTime.now())
            .flatMap(size -> 
                reactiveRedisService.listRange(key, 0, 4, String.class)
                    .collectList()
                    .map(list -> Map.of(
                        "key", key,
                        "list_size", size,
                        "recent_items", list,
                        "message", "List operation successful!"
                    ))
            );
    }

    /**
     * Проверка информации о ключах
     */
    @GetMapping("/key-info/{key}")
    public Mono<Map<String, Object>> keyInfo(@PathVariable String key) {
        return reactiveRedisService.exists(key)
            .flatMap(exists -> {
                if (exists) {
                    return reactiveRedisService.getTtl(key)
                        .map(ttl -> Map.of(
                            "key", key,
                            "exists", true,
                            "ttl_seconds", ttl.getSeconds(),
                            "message", "Key exists with TTL"
                        ));
                } else {
                    return Mono.just(Map.of(
                        "key", key,
                        "exists", false,
                        "message", "Key does not exist"
                    ));
                }
            });
    }

    /**
     * Удаление ключа
     */
    @DeleteMapping("/evict/{key}")
    public Mono<Map<String, Object>> evictKey(@PathVariable String key) {
        return reactiveRedisService.evict(key)
            .map(deleted -> Map.of(
                "key", key,
                "deleted", deleted,
                "message", deleted ? "Key deleted successfully" : "Key was not found"
            ));
    }

    // Вспомогательный класс для тестирования сериализации объектов
    public static class TestObject {
        private String name;
        private Integer number;
        private LocalDateTime timestamp;
        private Boolean active;

        public TestObject() {}

        public TestObject(String name, Integer number, LocalDateTime timestamp, Boolean active) {
            this.name = name;
            this.number = number;
            this.timestamp = timestamp;
            this.active = active;
        }

        // Геттеры и сеттеры
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Integer getNumber() { return number; }
        public void setNumber(Integer number) { this.number = number; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
} 