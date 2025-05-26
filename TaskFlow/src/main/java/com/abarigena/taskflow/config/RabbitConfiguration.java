package com.abarigena.taskflow.config;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Setter
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RabbitConfiguration {

    @Value("${queue.name}")
    private String queueName;

    @Value("${exchange.name}")
    private String exchangeName;

    @Value("${routing.key}")
    private String routingKey;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    // 1 задача
    @Bean
    public Queue queue() {
        return new Queue(queueName, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue()).to(exchange()).with(routingKey);
    }
    // ----
    // exchange

    @Bean
    public DirectExchange directExchange(){
        return new DirectExchange("taskflow.direct.exchange");
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("taskflow.fanout.exchange");
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("taskflow.topic.exchange");
    }

    @Bean
    public HeadersExchange headersExchange() {
        return new HeadersExchange("taskflow.headers.exchange");
    }

    // Dead letter Exchange
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("taskflow.dlx.exchange");
    }

    //new queue
    @Bean
    public Queue taskNotificationsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "taskflow.dlx.exchange");
        args.put("x-dead-letter-routing-key", "deadletter");
        return QueueBuilder.durable("task.notifications")
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue taskAuditFanoutQueue() {
        return QueueBuilder.durable("task.audit.fanout").build();
    }

    @Bean
    public Queue taskNotificationsTopicQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "taskflow.dlx.exchange");
        args.put("x-dead-letter-routing-key", "deadletter");
        return QueueBuilder.durable("task.notifications.topic")
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue taskErrorTopicQueue() {
        return QueueBuilder.durable("task.error.topic").build();
    }

    @Bean
    public Queue taskNotificationsHeadersQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "taskflow.dlx.exchange");
        args.put("x-dead-letter-routing-key", "deadletter");
        return QueueBuilder.durable("task.notifications.headers")
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("task.dlx.notifications").build();
    }

    //new Bindings
    @Bean
    public Binding directNotificationBinding() {
        return BindingBuilder.bind(taskNotificationsQueue())
                .to(directExchange())
                .with("task.notification");
    }

    @Bean
    public Binding fanoutAuditBinding() {
        return BindingBuilder.bind(taskAuditFanoutQueue())
                .to(fanoutExchange());
    }

    @Bean
    public Binding topicNotificationBinding() {
        return BindingBuilder.bind(taskNotificationsTopicQueue())
                .to(topicExchange())
                .with("task.notification.*");
    }

    @Bean
    public Binding topicErrorBinding() {
        return BindingBuilder.bind(taskErrorTopicQueue())
                .to(topicExchange())
                .with("task.error.*");
    }

    @Bean
    public Binding headersBinding() {
        return BindingBuilder.bind(taskNotificationsHeadersQueue())
                .to(headersExchange())
                .whereAll(Map.of("type", "notification")).match();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("deadletter");
    }

    @Bean(name = "rabbitConnectionFactory")
    public CachingConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin() {
        return new RabbitAdmin(rabbitConnectionFactory());
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(rabbitConnectionFactory());
        rabbitTemplate.setMessageConverter(messageConverter());

        // Обработка подтверждений доставки
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("Сообщение успешно доставлено в очередь");
            } else {
                log.info("Доставка сообщения в очередь не удалась: {}", cause);
            }
        });

        // Обработка недоставленных сообщений
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.error("Сообщение не доставлено. Причина: {}, Сообщение: {}",
                    returnedMessage.getReplyText(),
                    returnedMessage.getMessage());
        });

        rabbitTemplate.setMandatory(true);

        return rabbitTemplate;
    }
}
