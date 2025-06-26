package com.abarigena.cdcconsumerservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Упрощенный контроллер для мониторинга статуса CDC системы
 */
@Slf4j
@RestController
@RequestMapping("/api/cdc")
@RequiredArgsConstructor
public class SimpleCdcStatusController {

    private final OpenSearchClient openSearchClient;

    /**
     * Простая проверка статуса системы
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Проверяем подключение к OpenSearch
            HealthResponse health = openSearchClient.cluster().health(HealthRequest.of(h -> h));
            
            status.put("service", "cdc-consumer-service");
            status.put("status", "running");
            status.put("opensearch", Map.of(
                "status", health.status().jsonValue(),
                "clusterName", health.clusterName(),
                "numberOfNodes", health.numberOfNodes()
            ));
            status.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ Статус CDC системы проверен успешно");
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("❌ Ошибка получения статуса CDC: {}", e.getMessage(), e);
            status.put("service", "cdc-consumer-service");
            status.put("status", "error");
            status.put("error", e.getMessage());
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(status);
        }
    }

    /**
     * Информация о сервисе
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("service", "cdc-consumer-service");
        info.put("description", "CDC Consumer для синхронизации данных с OpenSearch");
        info.put("version", "1.0.0");
        info.put("entities", new String[]{"tasks", "comments", "projects", "users"});
        info.put("operations", new String[]{"create", "update", "delete", "snapshot"});
        info.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(info);
    }
} 