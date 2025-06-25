package com.abarigena.cdcconsumerservice.service;

import com.abarigena.cdcconsumerservice.dto.CommentIndex;
import com.abarigena.cdcconsumerservice.dto.ProjectIndex;
import com.abarigena.cdcconsumerservice.dto.TaskIndex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.DeleteRequest;
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
     * Универсальный метод для индексирования бизнес-сущностей
     */
    public void indexEntity(String entityType, Map<String, Object> entityData) {
        try {
            String indexName = getIndexName(entityType);
            Object id = entityData.get("id");
            
            if (id == null) {
                log.error("❌ Отсутствует ID для сущности типа: {}", entityType);
                return;
            }
            
            // Добавляем метаданные индексации
            Map<String, Object> document = new HashMap<>(entityData);
            document.put("indexedAt", LocalDateTime.now().toString());
            document.put("@timestamp", LocalDateTime.now().toString());
            
            // Нормализуем данные в зависимости от типа сущности
            final Map<String, Object> finalDocument = normalizeEntityData(entityType, document);
            
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(indexName)
                .id(String.valueOf(id))
                .document(finalDocument)
            );
            
            openSearchClient.index(request);
            
            log.info("✅ Сущность {} с ID {} проиндексирована в OpenSearch (индекс: {})", 
                entityType, id, indexName);
                
        } catch (Exception e) {
            log.error("❌ Ошибка индексирования сущности {}: {}", entityType, e.getMessage(), e);
        }
    }

    /**
     * Удаляет сущность из OpenSearch
     */
    public void deleteEntity(String entityType, String entityId) {
        try {
            String indexName = getIndexName(entityType);
            
            DeleteRequest request = DeleteRequest.of(d -> d
                .index(indexName)
                .id(entityId)
            );
            
            openSearchClient.delete(request);
            
            log.info("🗑️ Сущность {} с ID {} удалена из OpenSearch (индекс: {})", 
                entityType, entityId, indexName);
                
        } catch (Exception e) {
            log.error("❌ Ошибка удаления сущности {} с ID {}: {}", entityType, entityId, e.getMessage(), e);
        }
    }

    /**
     * Определяет имя индекса для типа сущности
     */
    private String getIndexName(String entityType) {
        return switch (entityType.toLowerCase()) {
            case "tasks" -> "tasks";
            case "comments" -> "comments";
            case "projects" -> "projects";
            case "users" -> "users";
            default -> entityType.toLowerCase();
        };
    }

    /**
     * Нормализует данные сущности для корректного сохранения в OpenSearch
     */
    private Map<String, Object> normalizeEntityData(String entityType, Map<String, Object> data) {
        Map<String, Object> normalized = new HashMap<>(data);
        
        switch (entityType.toLowerCase()) {
            case "tasks":
                // Преобразуем enum'ы в строки для лучшего поиска
                if (normalized.get("status") != null) {
                    normalized.put("status", String.valueOf(normalized.get("status")));
                }
                if (normalized.get("priority") != null) {
                    normalized.put("priority", String.valueOf(normalized.get("priority")));
                }
                break;
            case "comments":
                // Для комментариев переименуем context в content для соответствия индексу
                if (normalized.get("context") != null) {
                    normalized.put("content", normalized.get("context"));
                    normalized.remove("context");
                }
                break;
            case "projects":
                // Преобразуем статус проекта в строку
                if (normalized.get("status") != null) {
                    normalized.put("status", String.valueOf(normalized.get("status")));
                }
                break;
            case "users":
                // Объединяем имя и фамилию для лучшего поиска
                String firstName = (String) normalized.get("first_name");
                String lastName = (String) normalized.get("last_name");
                if (firstName != null && lastName != null) {
                    normalized.put("fullName", firstName + " " + lastName);
                }
                break;
        }
        
        return normalized;
    }

    // Оставляем старые методы для обратной совместимости, но помечаем как deprecated
    @Deprecated
    public void syncUserEvent(Long userId, String eventType, String eventData) {
        log.warn("⚠️ Метод syncUserEvent устарел, используйте indexEntity для users");
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", userId);
            userData.put("eventType", eventType);
            userData.put("eventData", eventData);
            indexEntity("user-events", userData);
        } catch (Exception e) {
            log.error("❌ Ошибка в deprecated методе syncUserEvent: {}", e.getMessage(), e);
        }
    }

    @Deprecated
    public void indexUser(String userData) {
        log.warn("⚠️ Метод indexUser устарел, используйте indexEntity для users");
        try {
            JsonNode userNode = objectMapper.readTree(userData);
            Map<String, Object> userMap = objectMapper.convertValue(userNode, Map.class);
            indexEntity("users", userMap);
        } catch (Exception e) {
            log.error("❌ Ошибка в deprecated методе indexUser: {}", e.getMessage(), e);
        }
    }
}