package com.abarigena.cdcconsumerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для индексирования проектов в OpenSearch
 * Соответствует требованиям Task-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectIndex {
    
    private Long id;
    private String name;
    private String description;
    private String status; // ACTIVE, COMPLETED, ARCHIVED
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Метаданные для OpenSearch
    @Builder.Default
    private String indexName = "projects";
    private LocalDateTime indexedAt;
} 