package com.abarigena.cdcconsumerservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OutboxEventData {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("event_data")
    private String eventData; // JSON строка с бизнес-данными
    
    @JsonProperty("created_at")
    private Long createdAt;
} 