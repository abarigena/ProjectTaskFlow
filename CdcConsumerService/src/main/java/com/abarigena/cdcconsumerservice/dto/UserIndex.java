package com.abarigena.cdcconsumerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для индексирования пользователей в OpenSearch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIndex {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Дополнительные поля для удобного поиска
    private String fullName;
    
    // Метаданные для OpenSearch
    @Builder.Default
    private String indexName = "users";
    private LocalDateTime indexedAt;
} 