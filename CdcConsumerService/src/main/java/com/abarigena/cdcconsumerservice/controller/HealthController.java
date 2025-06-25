package com.abarigena.cdcconsumerservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "CDC Consumer Service",
            "timestamp", LocalDateTime.now().toString(),
            "description", "Сервис для обработки CDC событий через Debezium"
        );
    }
} 