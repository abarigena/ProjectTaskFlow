package com.abarigena.taskflow.storeNoSQL.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "log_entries")
public class LogEntry {

    @Id
    private ObjectId id;

    private LogLevel level; // INFO, WARN, ERROR

    private String message;

    private LocalDateTime timestamp;

    private Map<String, Object> context;

    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }
}
