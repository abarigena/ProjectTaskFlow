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
 * Health check контроллер для CDC сервиса
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class CdcHealthController {

    private final OpenSearchClient openSearchClient;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "CDC Consumer Service");
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        // Проверяем подключение к OpenSearch
        try {
            HealthResponse healthResponse = openSearchClient.cluster().health(
                HealthRequest.of(h -> h)
            );
            health.put("opensearch", Map.of(
                "status", healthResponse.status().toString(),
                "cluster_name", healthResponse.clusterName()
            ));
        } catch (Exception e) {
            log.error("OpenSearch health check failed: {}", e.getMessage());
            health.put("opensearch", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }
        
        return ResponseEntity.ok(health);
    }
} 