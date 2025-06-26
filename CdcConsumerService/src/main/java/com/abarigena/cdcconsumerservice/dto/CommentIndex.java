package com.abarigena.cdcconsumerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для индексирования комментариев в OpenSearch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentIndex {
    
    private Long id;
    private String content;
    private Long taskId;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Метаданные для OpenSearch
    @Builder.Default
    private String indexName = "comments";
    private LocalDateTime indexedAt;
} 