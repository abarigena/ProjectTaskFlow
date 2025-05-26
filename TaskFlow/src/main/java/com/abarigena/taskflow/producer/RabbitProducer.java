package com.abarigena.taskflow.producer;

import com.abarigena.taskflow.dto.TaskHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitProducer {

    // 1 задача
    @Value("${exchange.name}")
    private String exhangeName;

    @Value("${routing.key}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;

    public void sendTaskHistory(TaskHistoryDto taskHistoryDto) {
        try {
            taskHistoryDto.setTimestamp(LocalDateTime.now());

            log.info("Sending task history: " + taskHistoryDto);
            rabbitTemplate.convertAndSend(exhangeName, routingKey, taskHistoryDto);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Exception while sending task history: " + taskHistoryDto, e);
        }
    }
    // --

    @Value("${taskflow.exchange.direct}")   // taskflow.direct.exchange
    private String notificationDirectExchange;

    @Value("${taskflow.exchange.fanout}")   // taskflow.fanout.exchange
    private String notificationFanoutExchange;

    @Value("${taskflow.exchange.topic}")    // taskflow.topic.exchange
    private String notificationTopicExchange;

    @Value("${taskflow.routing.notification}") // task.notification (для Direct)
    private String notificationDirectRoutingKey;

    /**
     * Отправляет уведомление о СОЗДАНИИ задачи (для NotificationService).
     * Использует Direct Exchange.
     */
    public void sendCreateNotification(TaskHistoryDto taskHistoryDto) {
        try{
            taskHistoryDto.setTimestamp(LocalDateTime.now());
            log.info("Sending CREATE notification DIRECT exchange [{}], routing key [{}]: {}",
                    notificationDirectExchange, notificationDirectRoutingKey, taskHistoryDto);
            // Отправляем в Direct Exchange с ключом task.notification
            rabbitTemplate.convertAndSend(notificationDirectExchange, notificationDirectRoutingKey, taskHistoryDto);
        }catch (Exception e){
            log.error("Error sending create notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление об ОБНОВЛЕНИИ задачи (для NotificationService).
     * Использует Fanout Exchange.
     */
    public void sendUpdateNotification(TaskHistoryDto taskHistoryDto) {
        try {
            taskHistoryDto.setTimestamp(LocalDateTime.now());
            log.info("Sending UPDATE notification via FANOUT exchange [{}]: {}",
                    notificationFanoutExchange, taskHistoryDto);
            // Отправляем в Fanout Exchange БЕЗ routing key (или с пустой строкой)
            rabbitTemplate.convertAndSend(notificationFanoutExchange, "", taskHistoryDto);
        } catch (Exception e) {
            log.error("Error sending update notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление об УДАЛЕНИИ задачи (для NotificationService).
     * Использует Topic Exchange.
     * @param taskHistoryDto Данные истории
     * @param topicRoutingKey Ключ маршрутизации для Topic (например, "task.notification.deleted")
     */
    public void sendDeleteNotification(TaskHistoryDto taskHistoryDto, String topicRoutingKey) {
        try {
            taskHistoryDto.setTimestamp(LocalDateTime.now());
            log.info("Sending DELETE notification via TOPIC exchange [{}], routing key [{}]: {}",
                    notificationTopicExchange, topicRoutingKey, taskHistoryDto);
            // Отправляем в Topic Exchange с ПЕРЕДАННЫМ ключом
            rabbitTemplate.convertAndSend(notificationTopicExchange, topicRoutingKey, taskHistoryDto);
        } catch (Exception e) {
            log.error("Error sending delete notification: {}", e.getMessage(), e);
        }
    }
}
