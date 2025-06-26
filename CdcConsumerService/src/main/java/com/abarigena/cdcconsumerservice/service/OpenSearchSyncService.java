package com.abarigena.cdcconsumerservice.service;

import com.abarigena.cdcconsumerservice.dto.CommentIndex;
import com.abarigena.cdcconsumerservice.dto.ProjectIndex;
import com.abarigena.cdcconsumerservice.dto.TaskIndex;
import com.abarigena.cdcconsumerservice.dto.UserIndex;
import com.abarigena.cdcconsumerservice.mapper.IndexMapper;
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

/**
 * Сервис для синхронизации данных с OpenSearch
 * 
 * Основные возможности:
 * - Типизированное индексирование через DTO (рекомендуется)
 * - Универсальное индексирование через Map (для совместимости)
 * - Удаление сущностей из индексов
 * - Автоматическая нормализация данных
 * 
 * Примеры использования:
 * 
 * 1. Индексирование пользователя (типизированный подход):
 * <pre>
 * {@code
 * UserIndex user = UserIndex.builder()
 *     .id(1L)
 *     .firstName("Иван")
 *     .lastName("Петров")
 *     .email("ivan@example.com")
 *     .active(true)
 *     .build();
 * 
 * openSearchSyncService.indexUser(user);
 * }
 * </pre>
 * 
 * 2. Универсальное индексирование из CDC события:
 * <pre>
 * {@code
 * Map<String, Object> userData = debeziumEvent.getAfter();
 * openSearchSyncService.indexEntityTyped("users", userData);
 * }
 * </pre>
 * 
 * 3. Удаление сущности:
 * <pre>
 * {@code
 * openSearchSyncService.deleteEntity("users", "1");
 * }
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchSyncService {

    private final OpenSearchClient openSearchClient;
    private final ObjectMapper objectMapper;
    private final IndexMapper indexMapper;

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
     * Улучшенный метод для индексирования сущностей с использованием типизированных DTO
     */
    public void indexEntityTyped(String entityType, Map<String, Object> entityData) {
        try {
            switch (entityType.toLowerCase()) {
                case "users":
                    UserIndex userIndex = indexMapper.mapToUserIndex(entityData);
                    indexUser(userIndex);
                    break;
                case "tasks":
                    TaskIndex taskIndex = indexMapper.mapToTaskIndex(entityData);
                    indexTask(taskIndex);
                    break;
                case "projects":
                    ProjectIndex projectIndex = indexMapper.mapToProjectIndex(entityData);
                    indexProject(projectIndex);
                    break;
                case "comments":
                    CommentIndex commentIndex = indexMapper.mapToCommentIndex(entityData);
                    indexComment(commentIndex);
                    break;
                default:
                    log.warn("⚠️ Неизвестный тип сущности: {}, используем универсальный метод", entityType);
                    indexEntity(entityType, entityData);
            }
        } catch (Exception e) {
            log.error("❌ Ошибка типизированного индексирования сущности {}: {}", entityType, e.getMessage(), e);
            // Fallback к универсальному методу
            log.info("🔄 Попытка индексирования через универсальный метод...");
            indexEntity(entityType, entityData);
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

    /**
     * Типизированный метод для индексирования пользователей
     */
    public void indexUser(UserIndex userIndex) {
        try {
            // Вычисляем fullName если он не задан
            if (userIndex.getFullName() == null && 
                userIndex.getFirstName() != null && 
                userIndex.getLastName() != null) {
                userIndex.setFullName(userIndex.getFirstName() + " " + userIndex.getLastName());
            }
            
            // Устанавливаем метаданные индексации
            userIndex.setIndexedAt(LocalDateTime.now());
            
            IndexRequest<UserIndex> request = IndexRequest.of(i -> i
                .index(userIndex.getIndexName())
                .id(String.valueOf(userIndex.getId()))
                .document(userIndex)
            );
            
            openSearchClient.index(request);
            
            log.info("✅ Пользователь {} (ID: {}) проиндексирован в OpenSearch", 
                userIndex.getFullName(), userIndex.getId());
                
        } catch (Exception e) {
            log.error("❌ Ошибка индексирования пользователя с ID {}: {}", 
                userIndex.getId(), e.getMessage(), e);
        }
    }

    /**
     * Типизированный метод для индексирования задач
     */
    public void indexTask(TaskIndex taskIndex) {
        try {
            taskIndex.setIndexedAt(LocalDateTime.now());
            
            IndexRequest<TaskIndex> request = IndexRequest.of(i -> i
                .index(taskIndex.getIndexName())
                .id(String.valueOf(taskIndex.getId()))
                .document(taskIndex)
            );
            
            openSearchClient.index(request);
            
            log.info("✅ Задача '{}' (ID: {}) проиндексирована в OpenSearch", 
                taskIndex.getTitle(), taskIndex.getId());
                
        } catch (Exception e) {
            log.error("❌ Ошибка индексирования задачи с ID {}: {}", 
                taskIndex.getId(), e.getMessage(), e);
        }
    }

    /**
     * Типизированный метод для индексирования проектов
     */
    public void indexProject(ProjectIndex projectIndex) {
        try {
            projectIndex.setIndexedAt(LocalDateTime.now());
            
            IndexRequest<ProjectIndex> request = IndexRequest.of(i -> i
                .index(projectIndex.getIndexName())
                .id(String.valueOf(projectIndex.getId()))
                .document(projectIndex)
            );
            
            openSearchClient.index(request);
            
            log.info("✅ Проект '{}' (ID: {}) проиндексирован в OpenSearch", 
                projectIndex.getName(), projectIndex.getId());
                
        } catch (Exception e) {
            log.error("❌ Ошибка индексирования проекта с ID {}: {}", 
                projectIndex.getId(), e.getMessage(), e);
        }
    }

    /**
     * Типизированный метод для индексирования комментариев
     */
    public void indexComment(CommentIndex commentIndex) {
        try {
            commentIndex.setIndexedAt(LocalDateTime.now());
            
            IndexRequest<CommentIndex> request = IndexRequest.of(i -> i
                .index(commentIndex.getIndexName())
                .id(String.valueOf(commentIndex.getId()))
                .document(commentIndex)
            );
            
            openSearchClient.index(request);
            
            log.info("✅ Комментарий (ID: {}) проиндексирован в OpenSearch", 
                commentIndex.getId());
                
        } catch (Exception e) {
            log.error("❌ Ошибка индексирования комментария с ID {}: {}", 
                commentIndex.getId(), e.getMessage(), e);
        }
    }

}