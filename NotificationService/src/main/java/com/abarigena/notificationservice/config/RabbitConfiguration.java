package com.abarigena.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import reactor.util.retry.Retry;

@Configuration
@Slf4j
public class RabbitConfiguration {

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    /**
     * Определяет обменник для недоставленных сообщений (DLX).
     * @return объект DirectExchange для DLX
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("taskflow.dlx.exchange");
    }

    /**
     * Определяет очередь для недоставленных сообщений (DLQ).
     * @return объект Queue для DLQ
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("task.dlx.notifications").build();
    }

    /**
     * Связывает очередь DLQ с обменником DLX.
     * @return объект Binding для DLQ и DLX
     */
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("deadletter");
    }

    /**
     * Конфигурирует фабрику соединений RabbitMQ с кэшированием.
     * @return объект CachingConnectionFactory
     */
    @Bean
    public CachingConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        return connectionFactory;
    }

    /**
     * Определяет конвертер сообщений для преобразования объектов в JSON и обратно.
     * @return объект MessageConverter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Конфигурирует RabbitAdmin для управления операциями на брокере RabbitMQ.
     * @param connectionFactory фабрика соединений RabbitMQ
     * @return объект RabbitAdmin
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        /**
         * Конфигурирует фабрику контейнеров слушателей RabbitMQ.
         * Устанавливает ручное подтверждение сообщений (MANUAL ACK) и предзагрузку (prefetch) в 1.
         * @param connectionFactory фабрика соединений RabbitMQ
         * @param messageConverter конвертер сообщений
         * @return объект SimpleRabbitListenerContainerFactory
         */
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);

        log.info("Configured SimpleRabbitListenerContainerFactory with MANUAL acknowledge mode and prefetch=1");
        return factory;
    }
}
