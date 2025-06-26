package com.abarigena.cdcconsumerservice.mapper;

import com.abarigena.cdcconsumerservice.dto.CommentIndex;
import com.abarigena.cdcconsumerservice.dto.ProjectIndex;
import com.abarigena.cdcconsumerservice.dto.TaskIndex;
import com.abarigena.cdcconsumerservice.dto.UserIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Маппер для преобразования данных из CDC событий в Index DTO для OpenSearch
 */
@Slf4j
@Component
public class IndexMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Преобразует Map в UserIndex
     */
    public UserIndex mapToUserIndex(Map<String, Object> userData) {
        try {
            return UserIndex.builder()
                .id(getLongValue(userData, "id"))
                .firstName(getStringValue(userData, "first_name"))
                .lastName(getStringValue(userData, "last_name"))
                .email(getStringValue(userData, "email"))
                .active(getBooleanValue(userData, "active"))
                .createdAt(getLocalDateTimeValue(userData, "created_at"))
                .updatedAt(getLocalDateTimeValue(userData, "updated_at"))
                .build();
        } catch (Exception e) {
            log.error("❌ Ошибка преобразования данных пользователя: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось преобразовать данные пользователя", e);
        }
    }

    /**
     * Преобразует Map в TaskIndex
     */
    public TaskIndex mapToTaskIndex(Map<String, Object> taskData) {
        try {
            return TaskIndex.builder()
                .id(getLongValue(taskData, "id"))
                .title(getStringValue(taskData, "title"))
                .description(getStringValue(taskData, "description"))
                .status(getStringValue(taskData, "status"))
                .priority(getStringValue(taskData, "priority"))
                .deadline(getLocalDateTimeValue(taskData, "deadline"))
                .assignedUserId(getLongValue(taskData, "assigned_user_id"))
                .projectId(getLongValue(taskData, "project_id"))
                .createdAt(getLocalDateTimeValue(taskData, "created_at"))
                .updatedAt(getLocalDateTimeValue(taskData, "updated_at"))
                .build();
        } catch (Exception e) {
            log.error("❌ Ошибка преобразования данных задачи: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось преобразовать данные задачи", e);
        }
    }

    /**
     * Преобразует Map в ProjectIndex
     */
    public ProjectIndex mapToProjectIndex(Map<String, Object> projectData) {
        try {
            return ProjectIndex.builder()
                .id(getLongValue(projectData, "id"))
                .name(getStringValue(projectData, "name"))
                .description(getStringValue(projectData, "description"))
                .status(getStringValue(projectData, "status"))
                .ownerId(getLongValue(projectData, "owner_id"))
                .createdAt(getLocalDateTimeValue(projectData, "created_at"))
                .updatedAt(getLocalDateTimeValue(projectData, "updated_at"))
                .build();
        } catch (Exception e) {
            log.error("❌ Ошибка преобразования данных проекта: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось преобразовать данные проекта", e);
        }
    }

    /**
     * Преобразует Map в CommentIndex
     */
    public CommentIndex mapToCommentIndex(Map<String, Object> commentData) {
        try {
            return CommentIndex.builder()
                .id(getLongValue(commentData, "id"))
                .content(getStringValue(commentData, "content"))
                .taskId(getLongValue(commentData, "task_id"))
                .userId(getLongValue(commentData, "user_id"))
                .createdAt(getLocalDateTimeValue(commentData, "created_at"))
                .updatedAt(getLocalDateTimeValue(commentData, "updated_at"))
                .build();
        } catch (Exception e) {
            log.error("❌ Ошибка преобразования данных комментария: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось преобразовать данные комментария", e);
        }
    }

    // Утилитные методы для безопасного извлечения значений из Map
    
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.warn("⚠️ Не удалось преобразовать значение '{}' в Long для ключа '{}'", value, key);
            return null;
        }
    }

    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private LocalDateTime getLocalDateTimeValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        
        try {
            // Если это число (микросекунды с эпохи), преобразуем
            if (value instanceof Number) {
                long microseconds = ((Number) value).longValue();
                // Преобразуем микросекунды в миллисекунды
                long milliseconds = microseconds / 1000;
                return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(milliseconds),
                    java.time.ZoneId.systemDefault()
                );
            }
            
            String stringValue = String.valueOf(value);
            // Если строка содержит только цифры, это микросекунды
            if (stringValue.matches("\\d+")) {
                long microseconds = Long.parseLong(stringValue);
                long milliseconds = microseconds / 1000;
                return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(milliseconds),
                    java.time.ZoneId.systemDefault()
                );
            }
            
            // Пробуем различные форматы даты
            return LocalDateTime.parse(stringValue, FORMATTER);
        } catch (Exception e) {
            log.warn("⚠️ Не удалось преобразовать значение '{}' в LocalDateTime для ключа '{}'", value, key);
            return null;
        }
    }
} 