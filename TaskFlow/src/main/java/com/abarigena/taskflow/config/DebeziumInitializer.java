package com.abarigena.taskflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Автоматическая инициализация Debezium коннектора при запуске приложения
 * Создает CDC коннектор для отслеживания изменений в бизнес-таблицах
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumInitializer implements ApplicationRunner {

    private final ResourceLoader resourceLoader;
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Создаем отдельный ObjectMapper для работы с Debezium API
    private final ObjectMapper debeziumObjectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${debezium.connect.url}")
    private String debeziumUrl;

    @Value("${debezium.connect.connector.name}")
    private String connectorName;

    @Value("${debezium.connect.connector.config-file}")
    private String configFile;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("🚀 Инициализация Debezium CDC коннектора...");
        
        // Ждем пока Debezium Connect будет готов
        waitForDebeziumConnect();
        
        // Проверяем существует ли коннектор
        if (isConnectorExists()) {
            log.info("✅ Debezium коннектор '{}' уже существует", connectorName);
            logConnectorStatus();
        } else {
            log.info("📡 Создаем новый Debezium коннектор '{}'...", connectorName);
            createConnector();
        }
    }

    private void waitForDebeziumConnect() {
        int maxRetries = 30;
        int delay = 2000; // 2 секунды
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    debeziumUrl + "/connectors", String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Debezium Connect готов к работе");
                    return;
                }
            } catch (Exception e) {
                log.warn("⏳ Ожидание Debezium Connect... попытка {}/{}", i + 1, maxRetries);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Прервано ожидание Debezium Connect", ie);
                }
            }
        }
        throw new RuntimeException("Debezium Connect недоступен после " + maxRetries + " попыток");
    }

    private boolean isConnectorExists() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                debeziumUrl + "/connectors", String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<String> connectors = debeziumObjectMapper.readValue(
                    response.getBody(), List.class);
                return connectors != null && connectors.contains(connectorName);
            }
            return false;
        } catch (Exception e) {
            log.error("❌ Ошибка проверки существования коннектора: {}", e.getMessage());
            return false;
        }
    }

    private void createConnector() {
        try {
            // Загружаем конфигурацию из ресурсов
            Resource resource = resourceLoader.getResource(configFile);
            Map<String, Object> config = debeziumObjectMapper.readValue(
                resource.getInputStream(), Map.class);

            // Отправляем запрос на создание коннектора
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(config, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                debeziumUrl + "/connectors", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Debezium коннектор '{}' успешно создан!", connectorName);
                log.debug("📄 Ответ сервера: {}", response.getBody());
                
                // Ждем немного и проверяем статус
                Thread.sleep(3000);
                logConnectorStatus();
            } else {
                log.error("❌ Ошибка создания коннектора. HTTP статус: {}", response.getStatusCode());
                log.error("📄 Ответ сервера: {}", response.getBody());
            }
            
        } catch (Exception e) {
            log.error("❌ Ошибка создания Debezium коннектора: {}", e.getMessage(), e);
        }
    }

    private void logConnectorStatus() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                debeziumUrl + "/connectors/" + connectorName + "/status", String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> status = debeziumObjectMapper.readValue(
                    response.getBody(), Map.class);
                
                log.info("📊 Статус коннектора '{}': {}", connectorName, 
                    status.get("connector"));
                
                List<Map<String, Object>> tasks = (List<Map<String, Object>>) status.get("tasks");
                if (tasks != null && !tasks.isEmpty()) {
                    for (int i = 0; i < tasks.size(); i++) {
                        log.info("   📋 Task {}: {}", i, tasks.get(i).get("state"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Не удалось получить статус коннектора: {}", e.getMessage());
        }
    }
} 