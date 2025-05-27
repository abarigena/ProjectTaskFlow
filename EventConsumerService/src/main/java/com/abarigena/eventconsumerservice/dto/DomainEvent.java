package com.abarigena.eventconsumerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainEvent {
    private String eventType;
    private String entityId;
    private String entityType;
    private Object payload;
    private LocalDateTime createdAt = LocalDateTime.now();
}
