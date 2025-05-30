package com.abarigena.taskflow.producer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaLogProducerServiceTests {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaLogProducerService kafkaLogProducerService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> messageCaptor;

    private static final String TEST_TOPIC = "taskflow.logs";

    @BeforeEach
    void setUp() {

    }

    @Test
    void testSendLog_Success() {
        String level = "INFO";
        String message = "Test log message";
        Map<String, Object> contextDetails = new HashMap<>();
        contextDetails.put("class", "TestClass");
        contextDetails.put("method", "testMethod");

        kafkaLogProducerService.sendLog(level, message, contextDetails);

        verify(kafkaTemplate).send(eq(TEST_TOPIC), messageCaptor.capture());

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(level, capturedMessage.get("level"));
        assertEquals(message, capturedMessage.get("message"));
        assertEquals(contextDetails, capturedMessage.get("context"));
        assertTrue(capturedMessage.containsKey("timestamp"));

        LocalDateTime timestamp = LocalDateTime.parse((String) capturedMessage.get("timestamp"));
        assertTrue(timestamp.isBefore(LocalDateTime.now().plusSeconds(1)) && timestamp.isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    void testSendLog_EmptyContext() {
        String level = "WARN";
        String message = "Log with empty context";
        Map<String, Object> contextDetails = Collections.emptyMap();

        kafkaLogProducerService.sendLog(level, message, contextDetails);

        verify(kafkaTemplate).send(eq(TEST_TOPIC), messageCaptor.capture());

        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(level, capturedMessage.get("level"));
        assertEquals(message, capturedMessage.get("message"));
        assertEquals(contextDetails, capturedMessage.get("context"));
        assertTrue(capturedMessage.containsKey("timestamp"));
    }

    @Test
    void testSendLog_KafkaTemplateThrowsException() {
        String level = "ERROR";
        String message = "Log that causes Kafka error";
        Map<String, Object> contextDetails = Collections.singletonMap("errorSource", "test");

        doThrow(new RuntimeException("Kafka send failed")).when(kafkaTemplate).send(anyString(), anyMap());

        assertDoesNotThrow(() -> kafkaLogProducerService.sendLog(level, message, contextDetails));

        verify(kafkaTemplate).send(eq(TEST_TOPIC), messageCaptor.capture());
        Map<String, Object> capturedMessage = messageCaptor.getValue();
        assertEquals(level, capturedMessage.get("level"));
    }
}
