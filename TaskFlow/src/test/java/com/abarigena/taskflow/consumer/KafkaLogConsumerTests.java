package com.abarigena.taskflow.consumer;

import com.abarigena.taskflow.serviceNoSQL.LogEntryService;
import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaLogConsumerTests {

    @Mock
    private LogEntryService logEntryService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private KafkaLogConsumer kafkaLogConsumer;

    @Captor
    private ArgumentCaptor<LogEntry> logEntryCaptor;

    @BeforeEach
    void setUp() {
    }

    private String createKafkaMessage(String level, String message, Map<String, Object> context, String timestamp) throws JsonProcessingException {
        Map<String, Object> logData = new HashMap<>();
        logData.put("level", level);
        logData.put("message", message);
        logData.put("context", context);
        logData.put("timestamp", timestamp);
        return new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(logData);
    }

    private void assertMapEquals(Map<String, Object> expected, Map<String, Object> actual) {
        assertNotNull(actual, "Actual map should not be null");
        assertEquals(expected.size(), actual.size(), "Maps should have the same size");

        for (Map.Entry<String, Object> expectedEntry : expected.entrySet()) {
            String key = expectedEntry.getKey();
            Object expectedValue = expectedEntry.getValue();

            assertTrue(actual.containsKey(key), "Actual map should contain key: " + key);
            Object actualValue = actual.get(key);

            if (expectedValue instanceof Number && actualValue instanceof Number) {
                assertEquals(((Number) expectedValue).longValue(), ((Number) actualValue).longValue(),
                        "Numeric value for key '" + key + "' should match");
            } else {
                assertEquals(expectedValue, actualValue, "Value for key '" + key + "' should match");
            }
        }
    }

    @Test
    void testListenToLogTopic_ValidTaskCreatedMessage() throws JsonProcessingException {
        String level = "INFO";
        String msg = "Task created";
        Map<String, Object> context = new HashMap<>();
        context.put("taskId", 123L);
        context.put("title", "New Task");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String kafkaMessage = createKafkaMessage(level, msg, context, timestamp);

        when(logEntryService.saveLog(any(LogEntry.class))).thenReturn(Mono.just(new LogEntry()));

        kafkaLogConsumer.listenToLogTopic(kafkaMessage);

        verify(logEntryService).saveLog(logEntryCaptor.capture());
        LogEntry capturedLogEntry = logEntryCaptor.getValue();

        assertEquals(LogEntry.LogLevel.INFO, capturedLogEntry.getLevel());
        assertEquals(msg, capturedLogEntry.getMessage());
        assertMapEquals(context, capturedLogEntry.getContext());
        assertEquals(LocalDateTime.parse(timestamp), capturedLogEntry.getTimestamp());
    }

    @Test
    void testListenToLogTopic_ValidProjectUpdatedMessage() throws JsonProcessingException {
        String level = "INFO";
        String msg = "Project updated";
        Map<String, Object> context = new HashMap<>();
        context.put("projectId", 456L);
        context.put("name", "Updated Project Name");
        context.put("status", "ACTIVE");
        String timestamp = LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String kafkaMessage = createKafkaMessage(level, msg, context, timestamp);

        when(logEntryService.saveLog(any(LogEntry.class))).thenReturn(Mono.just(new LogEntry()));

        kafkaLogConsumer.listenToLogTopic(kafkaMessage);

        verify(logEntryService).saveLog(logEntryCaptor.capture());
        LogEntry capturedLogEntry = logEntryCaptor.getValue();

        assertEquals(LogEntry.LogLevel.INFO, capturedLogEntry.getLevel());
        assertEquals(msg, capturedLogEntry.getMessage());
        assertMapEquals(context, capturedLogEntry.getContext());
        assertEquals(LocalDateTime.parse(timestamp), capturedLogEntry.getTimestamp());
    }

    @Test
    void testListenToLogTopic_ValidCommentDeletedMessage() throws JsonProcessingException {
        String level = "INFO";
        String msg = "Comment deleted";
        Map<String, Object> context = new HashMap<>();
        context.put("commentId", 789L);
        context.put("taskId", 101L);
        String timestamp = LocalDateTime.now().minusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String kafkaMessage = createKafkaMessage(level, msg, context, timestamp);

        when(logEntryService.saveLog(any(LogEntry.class))).thenReturn(Mono.just(new LogEntry()));

        kafkaLogConsumer.listenToLogTopic(kafkaMessage);

        verify(logEntryService).saveLog(logEntryCaptor.capture());
        LogEntry capturedLogEntry = logEntryCaptor.getValue();

        assertEquals(LogEntry.LogLevel.INFO, capturedLogEntry.getLevel());
        assertEquals(msg, capturedLogEntry.getMessage());
        assertMapEquals(context, capturedLogEntry.getContext());
        assertEquals(LocalDateTime.parse(timestamp), capturedLogEntry.getTimestamp());
    }

    @Test
    void testListenToLogTopic_MalformedJson() {
        String malformedKafkaMessage = "{\"level\":\"INFO\", \"message\":\"Test message\", \"context\":{}";

        assertDoesNotThrow(() -> kafkaLogConsumer.listenToLogTopic(malformedKafkaMessage));

        verify(logEntryService, never()).saveLog(any(LogEntry.class));
    }

    @Test
    void testListenToLogTopic_InvalidLogLevel() throws JsonProcessingException {
        String level = "DEBUGS";
        String msg = "Test with invalid level";
        Map<String, Object> context = Collections.singletonMap("key", "value");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String kafkaMessage = createKafkaMessage(level, msg, context, timestamp);

        when(logEntryService.saveLog(any(LogEntry.class))).thenReturn(Mono.just(new LogEntry()));

        kafkaLogConsumer.listenToLogTopic(kafkaMessage);

        verify(logEntryService).saveLog(logEntryCaptor.capture());
        LogEntry capturedLogEntry = logEntryCaptor.getValue();

        assertEquals(LogEntry.LogLevel.INFO, capturedLogEntry.getLevel());
        assertEquals(msg, capturedLogEntry.getMessage());
    }
    
    @Test
    void testListenToLogTopic_NullLogLevel() throws JsonProcessingException {
        String msg = "Test with null level";
        Map<String, Object> context = Collections.singletonMap("key", "value");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        Map<String, Object> logData = new HashMap<>();
        logData.put("level", null);
        logData.put("message", msg);
        logData.put("context", context);
        logData.put("timestamp", timestamp);
        String kafkaMessage = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(logData);

        when(logEntryService.saveLog(any(LogEntry.class))).thenReturn(Mono.just(new LogEntry()));

        kafkaLogConsumer.listenToLogTopic(kafkaMessage);

        verify(logEntryService).saveLog(logEntryCaptor.capture());
        LogEntry capturedLogEntry = logEntryCaptor.getValue();

        assertEquals(LogEntry.LogLevel.INFO, capturedLogEntry.getLevel());
        assertEquals(msg, capturedLogEntry.getMessage());
    }

    @Test
    void testListenToLogTopic_InvalidTimestampFormat() throws JsonProcessingException {
        String level = "WARN";
        String msg = "Test with invalid timestamp";
        Map<String, Object> context = Collections.singletonMap("timestampTest", true);
        String invalidTimestamp = "2023-10-26T10:15:30.12345.123";
        String kafkaMessage = createKafkaMessage(level, msg, context, invalidTimestamp);
        
        LocalDateTime beforeCall = LocalDateTime.now();
        when(logEntryService.saveLog(any(LogEntry.class))).thenReturn(Mono.just(new LogEntry()));

        kafkaLogConsumer.listenToLogTopic(kafkaMessage);
        LocalDateTime afterCall = LocalDateTime.now();


        verify(logEntryService).saveLog(logEntryCaptor.capture());
        LogEntry capturedLogEntry = logEntryCaptor.getValue();

        assertNotNull(capturedLogEntry.getTimestamp());
        assertTrue(capturedLogEntry.getTimestamp().isAfter(beforeCall.minusSeconds(1))
                && capturedLogEntry.getTimestamp().isBefore(afterCall.plusSeconds(1)) );
        assertEquals(LogEntry.LogLevel.WARN, capturedLogEntry.getLevel());
    }
    
    @Test
    void testListenToLogTopic_NullTimestamp() throws JsonProcessingException {
        String level = "ERROR";
        String msg = "Test with null timestamp";
        Map<String, Object> context = Collections.singletonMap("timestampTest", "null_case");
        
        Map<String, Object> logData = new HashMap<>();
        logData.put("level", level);
        logData.put("message", msg);
        logData.put("context", context);
        logData.put("timestamp", null);
        String kafkaMessage = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(logData);
        
        LocalDateTime beforeCall = LocalDateTime.now();
        when(logEntryService.saveLog(any(LogEntry.class))).thenReturn(Mono.just(new LogEntry()));

        kafkaLogConsumer.listenToLogTopic(kafkaMessage);
        LocalDateTime afterCall = LocalDateTime.now();

        verify(logEntryService).saveLog(logEntryCaptor.capture());
        LogEntry capturedLogEntry = logEntryCaptor.getValue();

        assertNotNull(capturedLogEntry.getTimestamp());
        assertTrue(capturedLogEntry.getTimestamp().isAfter(beforeCall.minusSeconds(1))
                && capturedLogEntry.getTimestamp().isBefore(afterCall.plusSeconds(1)) );
        assertEquals(LogEntry.LogLevel.ERROR, capturedLogEntry.getLevel());
    }

    @Test
    void testListenToLogTopic_LogEntryServiceThrowsError() throws JsonProcessingException {
        String level = "INFO";
        String msg = "Service error test";
        Map<String, Object> context = Collections.singletonMap("errorSim", true);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String kafkaMessage = createKafkaMessage(level, msg, context, timestamp);

        when(logEntryService.saveLog(any(LogEntry.class))).thenReturn(Mono.error(new RuntimeException("DB error")));

        assertDoesNotThrow(() -> kafkaLogConsumer.listenToLogTopic(kafkaMessage));

        verify(logEntryService).saveLog(any(LogEntry.class));
    }
}
