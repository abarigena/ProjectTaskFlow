package com.abarigena.taskflow.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaLogProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String KAFKA_TOPIC = "taskflow.logs";

    public void sendLog(String level, String message, Map<String, Object> contextDetails) {
        Map<String, Object> logMessage = new HashMap<>();
        logMessage.put("level", level);
        logMessage.put("message", message);
        logMessage.put("context", contextDetails);
        logMessage.put("timestamp", LocalDateTime.now().toString());

        try {
            kafkaTemplate.send(KAFKA_TOPIC, logMessage);
            log.info("Сообщение лога успешно отправлено в Kafka топик {}: {}", KAFKA_TOPIC, logMessage);
        } catch (Exception e) {
            log.error("Ошибка отправки сообщения лога в Kafka топик {}: {}", KAFKA_TOPIC, logMessage, e);
        }
    }
}
