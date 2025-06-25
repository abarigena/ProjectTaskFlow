package com.abarigena.cdcconsumerservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchSyncService {

    private final OpenSearchClient openSearchClient;
    private final ObjectMapper objectMapper;

    /**
     * Синхронизирует событие пользователя с OpenSearch
     */
    public void syncUserEvent(Long userId, String eventType, String eventData) {
        try {
            // Парсим JSON данные события
            JsonNode dataNode = objectMapper.readTree(eventData);
            
            // Создаем документ для индексации
            Map<String, Object> document = new HashMap<>();
            document.put("userId", userId);
            document.put("eventType", eventType);
            document.put("eventData", dataNode);
            document.put("timestamp", LocalDateTime.now().toString());
            document.put("@timestamp", LocalDateTime.now().toString()); // Для OpenSearch Dashboards time-based индекса
            
            // Индексируем в OpenSearch  
            String indexName = "user-events-" + LocalDateTime.now().toString().substring(0, 7); // user-events-2025-01
            
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(indexName)
                .document(document)
            );
            
            openSearchClient.index(request);
            
            log.info("📊 Событие синхронизировано с OpenSearch: userId={}, eventType={}, index={}", 
                userId, eventType, indexName);
                
        } catch (Exception e) {
            log.error("❌ Ошибка синхронизации с OpenSearch: {}", e.getMessage(), e);
        }
    }

    /**
     * Создает пользователя в OpenSearch для поиска
     */
    public void indexUser(String userData) {
        try {
            JsonNode userNode = objectMapper.readTree(userData);
            
            Map<String, Object> userDocument = new HashMap<>();
            userDocument.put("userId", userNode.get("userId").asLong());
            userDocument.put("name", userNode.get("name").asText());
            userDocument.put("email", userNode.get("email").asText());
            userDocument.put("createdAt", LocalDateTime.now().toString());
            userDocument.put("@timestamp", LocalDateTime.now().toString());
            
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index("users")
                .id(String.valueOf(userNode.get("userId").asLong()))
                .document(userDocument)
            );
            
            openSearchClient.index(request);
            
            log.info("👤 Пользователь проиндексирован в OpenSearch: {}", userNode.get("name").asText());
            
        } catch (Exception e) {
            log.error("❌ Ошибка индексации пользователя: {}", e.getMessage(), e);
        }
    }
}