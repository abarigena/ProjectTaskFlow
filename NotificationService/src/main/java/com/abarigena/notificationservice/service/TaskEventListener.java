package com.abarigena.notificationservice.service;

import com.abarigena.notificationservice.dto.TaskHistoryDto;
import com.abarigena.notificationservice.store.entity.NotificationTaskHistory;
import com.abarigena.notificationservice.store.repository.NotificationTaskHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener; // Аннотация для слушателя
import org.springframework.amqp.support.AmqpHeaders; // Константы для заголовков AMQP
import org.springframework.amqp.core.Message;
import org.springframework.messaging.handler.annotation.Header; // Аннотация для извлечения заголовка
import org.springframework.messaging.handler.annotation.Payload; // Аннотация для извлечения тела сообщения
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class TaskEventListener {
    private final NotificationTaskHistoryRepository repository;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            queues = "${taskflow.queue.notifications}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleTaskCreateNotification(
            @Payload TaskHistoryDto message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
    {
        String queueName = "task.notifications";
        log.info("<<< Получено CREATE сообщение из очереди [{}], deliveryTag [{}]: {}", queueName, deliveryTag, message);

        processAndSave(message, channel, deliveryTag, queueName);
    }

    @RabbitListener(
            queues = "${taskflow.queue.audit}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleTaskUpdateNotification(
            @Payload TaskHistoryDto message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ){
        String queueName = "task.audit.fanout";
        log.info("<<< Получено UPDATE сообщение из очереди[{}], deliveryTag [{}]: {}", queueName, deliveryTag, message);
        processAndSave(message, channel, deliveryTag, queueName);
    }

    @RabbitListener(
            queues = "${taskflow.queue.notifications-topic}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleTaskDeleteNotification(
            @Payload TaskHistoryDto message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ){
        String queueName = "task.notifications.topic";
        log.info("<<< Получено DELETE сообщение из очереди [{}], deliveryTag [{}]: {}", queueName, deliveryTag, message);
        processAndSave(message, channel, deliveryTag, queueName);
    }

    private void processAndSave(TaskHistoryDto message, Channel channel,
                                long deliveryTag, String queueName){

        if (message.getTaskId() == null || message.getAction() == null) {
            log.error("!!! Invalid message received from queue [{}]: taskId or action is null. Throwing exception.", queueName);
            rejectMessage(channel, deliveryTag, queueName, false);
            return;
        }

        NotificationTaskHistory entityToSave = mapDtoToEntity(message);
        log.debug("Mapped DTO to entity: {}", entityToSave);

        log.debug("Attempting to save entity for deliveryTag [{}]...", deliveryTag);

        repository.save(entityToSave)
                .doOnSuccess(savedEntity -> {
                    log.info(">>> Successfully saved notification with ID: {} for deliveryTag [{}]", savedEntity.getId(), deliveryTag);
                    acknowledgeMessage(channel, deliveryTag, queueName);
                })
                .doOnError(error -> {
                    log.error("!!! Failed to save notification to DB for deliveryTag [{}]: {}", deliveryTag, error.getMessage(), error);
                    rejectMessage(channel, deliveryTag, queueName, false);
                })
                .subscribe(
                        savedEntity -> {},
                        error -> {
                            log.debug("Error from reactive chain for deliveryTag [{}]: {}", deliveryTag, error.getMessage());
                        }
                );
    }

    @RabbitListener(
            queues = "${taskflow.queue.dlx}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleDeadLetter(
            Message failedMessage,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException{
        String dlqName = "task.dlx.notifications";
        log.error("<<< Received message in DLQ [{}], deliveryTag [{}].", dlqName, deliveryTag);

        Map<String, Object> headers = failedMessage.getMessageProperties().getHeaders();
        Object originalExchange = headers.get("x-original-exchange");
        Object originalRoutingKey = headers.get("x-original-routing-key");
        Object firstDeathExchange = headers.get("x-first-death-exchange");
        Object firstDeathQueue = headers.get("x-first-death-queue");
        Object firstDeathReason = headers.get("x-first-death-reason");
        Object deathCount = headers.get("x-death-count");

        log.error("    Original Exchange: {}", originalExchange);
        log.error("    Original RoutingKey: {}", originalRoutingKey);
        log.error("    First Death Exchange: {}", firstDeathExchange);
        log.error("    First Death Queue: {}", firstDeathQueue);
        log.error("    First Death Reason: {}", firstDeathReason);
        log.error("    Death Count: {}", deathCount);

        String body = new String(failedMessage.getBody());
        log.error("   Message body: {}", body);

        /*log.warn("--- Ack'ing (removing) message from DLQ [{}] ---", dlqName);
        channel.basicAck(deliveryTag, false);*/
    }

    private void acknowledgeMessage(Channel channel, long deliveryTag, String queueName) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void rejectMessage(Channel channel, long deliveryTag, String queueName, boolean requeue) {
        try{
            channel.basicNack(deliveryTag, false, requeue);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    // TODO заменить на маппер
    private NotificationTaskHistory mapDtoToEntity(TaskHistoryDto dto) {
        String actionString = (dto.getAction() != null) ? dto.getAction().name() : null;
        String detailsJson = convertMapToJsonString(dto.getDetails());

        return NotificationTaskHistory.builder()
                .taskId(dto.getTaskId())
                .action(actionString)
                .performedBy(dto.getPerformedBy())
                .status(dto.getStatus())
                .timestamp(dto.getTimestamp())
                .details(detailsJson)
                .build();
    }

    private String convertMapToJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        }catch (JsonProcessingException e){
            throw new RuntimeException("Error serializing task details", e);
        }
    }
}
