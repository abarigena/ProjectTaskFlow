package com.abarigena.taskflow.consumer;

import com.abarigena.taskflow.serviceNoSQL.LogEntryService;
import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaLogConsumer {

    private final LogEntryService logEntryService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @KafkaListener(topics = "taskflow.logs", groupId = "taskflow_log_group")
    public void listenToLogTopic(String kafkaMessage) {
        log.info("Received message from Kafka topic 'taskflow.logs': {}", kafkaMessage);

        try {
            Map<String, Object> logData = objectMapper.readValue(kafkaMessage, new TypeReference<Map<String, Object>>() {});

            String levelStr = (String) logData.get("level");
            String message = (String) logData.get("message");
            String timestampStr = (String) logData.get("timestamp");
            Map<String, Object> context = (Map<String, Object>) logData.get("context");

            LogEntry logEntry = new LogEntry();
            logEntry.setMessage(message);
            logEntry.setContext(context);

            try {
                if (timestampStr != null) {
                    logEntry.setTimestamp(LocalDateTime.parse(timestampStr));
                } else {
                    logEntry.setTimestamp(LocalDateTime.now());
                }
            } catch (DateTimeParseException e) {
                log.error("Error parsing timestamp from Kafka message: {}. Using current time as fallback.", timestampStr, e);
                logEntry.setTimestamp(LocalDateTime.now());
            }

            try {
                if (levelStr != null) {
                    logEntry.setLevel(LogEntry.LogLevel.valueOf(levelStr.toUpperCase()));
                } else {
                    logEntry.setLevel(LogEntry.LogLevel.INFO);
                }
            } catch (IllegalArgumentException e) {
                log.error("Invalid log level string from Kafka: {}. Using INFO as fallback.", levelStr, e);
                logEntry.setLevel(LogEntry.LogLevel.INFO);
            }

            logEntryService.saveLog(logEntry)
                .subscribe(
                    savedEntry -> log.info("Successfully saved log to MongoDB with ID: {}", savedEntry.getId()),
                    error -> log.error("Error saving log to MongoDB from Kafka consumer: {}", error.getMessage(), error)
                );

        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON message from Kafka: {}", kafkaMessage, e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while processing Kafka message: {}", kafkaMessage, e);
        }
    }
}
