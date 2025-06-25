package com.abarigena.cdcconsumerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для индексирования задач в OpenSearch
 * Соответствует требованиям Task-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskIndex {
    
    private Long id;
    private String title;
    private String description;
    private String status;  // TODO, IN_PROGRESS, DONE
    private String priority; // LOW, MEDIUM, HIGH
    private LocalDateTime deadline;
    private Long assignedUserId;
    private Long projectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Метаданные для OpenSearch
    @Builder.Default
    private String indexName = "tasks";
    private LocalDateTime indexedAt;
} 