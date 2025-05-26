package com.abarigena.notificationservice.integration;

import com.abarigena.notificationservice.dto.TaskHistoryDto;
import com.abarigena.notificationservice.store.entity.NotificationTaskHistory;
import com.abarigena.notificationservice.store.repository.NotificationTaskHistoryRepository;
import com.rabbitmq.client.AMQP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TaskEventListenerTest.Initializer.class)
@DisplayName("Интеграционные тесты для TaskEventListener (с Testcontainers)")
class TaskEventListenerTest {

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse(
            "rabbitmq:3-management-alpine"
    )).withExposedPorts(5672, 15672).withAdminUser("rabbitmq").withAdminPassword("rabbitmq");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    NotificationTaskHistoryRepository notificationRepository;

    private static final String NOTIFICATIONS_QUEUE = "task.notifications";
    private static final String AUDIT_QUEUE = "task.audit.fanout";
    private static final String NOTIFICATIONS_TOPIC_QUEUE = "task.notifications.topic";

    private static final String DLX_EXCHANGE_NAME = "taskflow.dlx.exchange";
    private static final String DLQ_NAME = "task.dlx.notifications";

    private static final String DIRECT_EXCHANGE = "taskflow.direct.exchange";
    private static final String FANOUT_EXCHANGE = "taskflow.fanout.exchange";
    private static final String TOPIC_EXCHANGE = "taskflow.topic.exchange";

    private static final String NOTIFICATION_ROUTING_KEY = "task.notification";
    private static final String NOTIFICATION_TOPIC_DELETE_KEY = "task.notification.deleted";

    @TestConfiguration
    static class RabbitConfiguration{
        //exchange
        @Bean
        public DirectExchange testDirectExchange() {return new DirectExchange(DIRECT_EXCHANGE);}
        @Bean
        public FanoutExchange testFanoutExchange() {return new FanoutExchange(FANOUT_EXCHANGE);}
        @Bean
        public TopicExchange testTopicExchange() {return new TopicExchange(TOPIC_EXCHANGE);}
        @Bean
        public DirectExchange testDlxExchange() {return new DirectExchange(DLX_EXCHANGE_NAME);}

        //queues
        @Bean
        public Queue testNotificationsQueue(){
            return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                    .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                    .withArgument("x-dead-letter-routing-key", "deadletter")
                    .build();
        }
        @Bean
        public Queue testAuditQueue(){ return new Queue(AUDIT_QUEUE,true); }
        @Bean
        public Queue testNotificationsTopicQueue(){
            return QueueBuilder.durable(NOTIFICATIONS_TOPIC_QUEUE)
                    .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                    .withArgument("x-dead-letter-routing-key", "deadletter")
                    .build();
        }
        @Bean
        public Queue testDql(){return new Queue(DLQ_NAME,true); }

        // Bindings
        @Bean public Binding testDirectBinding(Queue testNotificationsQueue, DirectExchange testDirectExchange) {
            return BindingBuilder.bind(testNotificationsQueue).to(testDirectExchange).with(NOTIFICATION_ROUTING_KEY);
        }
        @Bean public Binding testFanoutBinding(Queue testAuditQueue, FanoutExchange testFanoutExchange) {
            return BindingBuilder.bind(testAuditQueue).to(testFanoutExchange);
        }
        @Bean public Binding testTopicBinding(Queue testNotificationsTopicQueue, TopicExchange testTopicExchange) {
            return BindingBuilder.bind(testNotificationsTopicQueue).to(testTopicExchange).with("task.notification.*");
        }
        @Bean public Binding testDlxBinding(Queue testDql, DirectExchange testDlxExchange) {
            return BindingBuilder.bind(testDql).to(testDlxExchange).with("deadletter");
        }
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s",
                    postgres.getHost(), postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgres.getDatabaseName());
            String jdbcUrl = postgres.getJdbcUrl(); // Для Liquibase

            // RabbitMQ
            String rabbitHost = rabbitmq.getHost();
            Integer rabbitPort = rabbitmq.getMappedPort(5672);

            TestPropertyValues.of(
                    // R2DBC для NotificationService
                    "spring.r2dbc.url=" + r2dbcUrl,
                    "spring.r2dbc.username=" + postgres.getUsername(),
                    "spring.r2dbc.password=" + postgres.getPassword(),
                    // Liquibase для NotificationService
                    "spring.liquibase.url=" + jdbcUrl,
                    "spring.liquibase.user=" + postgres.getUsername(),
                    "spring.liquibase.password=" + postgres.getPassword(),
                    "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml",
                    "spring.liquibase.enabled=true",
                    // RabbitMQ
                    "spring.rabbitmq.host=" + rabbitHost,
                    "spring.rabbitmq.port=" + rabbitPort,
                    "spring.rabbitmq.username=" + rabbitmq.getAdminUsername(),
                    "spring.rabbitmq.password=" + rabbitmq.getAdminPassword(),
                    "spring.rabbitmq.listener.simple.acknowledge-mode=manual",
                    "spring.rabbitmq.listener.simple.prefetch=1",
/*                    "spring.rabbitmq.listener.simple.retry.enabled=false",
                    "spring.rabbitmq.listener.simple.default-requeue-rejected=false",*/
                    "logging.level.org.springframework.amqp=DEBUG",
                    "logging.level.org.springframework.retry=DEBUG"
            ).applyTo(applicationContext.getEnvironment());
        }
    }

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll().block();

        clearQueue(NOTIFICATIONS_QUEUE);
        clearQueue(AUDIT_QUEUE);
        clearQueue(NOTIFICATIONS_TOPIC_QUEUE);
        clearQueue(DLQ_NAME);
    }

    private void clearQueue(String queueName) {
        RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());

        admin.purgeQueue(queueName, false);
    }

    @Test
    @DisplayName("Должен получить сообщение из 'task.notifications' и сохранить в БД")
    void shouldReceiveAndSaveNotification() throws InterruptedException {
        TaskHistoryDto testDto = TaskHistoryDto.builder()
                .taskId(1L)
                .action(TaskHistoryDto.Action.CREATE)
                .performedBy(100L)
                .status("TODO")
                .timestamp(LocalDateTime.now())
                .details(Map.of("title", "Тестовое уведомление"))
                .build();

        rabbitTemplate.convertAndSend(DIRECT_EXCHANGE, NOTIFICATION_ROUTING_KEY, testDto);

        Thread.sleep(2000);

        List<NotificationTaskHistory> savedNotifications = notificationRepository.findAll().collectList().block();

        assertThat(savedNotifications).hasSize(1);
        NotificationTaskHistory savedNotification = savedNotifications.get(0);
        assertThat(savedNotification.getTaskId()).isEqualTo(testDto.getTaskId());
        assertThat(savedNotification.getAction()).isEqualTo(testDto.getAction().name());
        assertThat(savedNotification.getStatus()).isEqualTo(testDto.getStatus());

        assertThat(savedNotification.getDetails()).isEqualTo("{\"title\":\"Тестовое уведомление\"}");
    }

    @Test
    @DisplayName("Должен переместить сообщение в DLQ при постоянной ошибке обработки")
    void shouldMoveMessageToDlxOnProcessingError() throws InterruptedException {
        TaskHistoryDto problematicDto = TaskHistoryDto.builder()
                .taskId(null)
                .action(TaskHistoryDto.Action.CREATE)
                .performedBy(101L)
                .status("INVALID_DATA")
                .timestamp(LocalDateTime.now())
                .details(Map.of("reason", "taskId was null"))
                .build();

        rabbitTemplate.convertAndSend(DIRECT_EXCHANGE, NOTIFICATION_ROUTING_KEY, problematicDto);

        Thread.sleep(2000);

        Object messageInMainQueue = rabbitTemplate.receive(NOTIFICATIONS_QUEUE, 100);

        assertThat(messageInMainQueue)
                .as("Сообщение не должно остаться в основной очереди %s", NOTIFICATIONS_QUEUE)
                .isNull();

        List<NotificationTaskHistory> savedNotifications = notificationRepository.findAll().collectList().block();
        assertThat(savedNotifications)
                .as("В БД не должно быть записей для этого сообщения (taskId=null)")
                .isEmpty();

        Message deadLetterAmqpMessage = rabbitTemplate.receive(DLQ_NAME,1000);
        assertThat(deadLetterAmqpMessage)
                .as("Сообщение должно быть в DLQ %s", DLQ_NAME)
                .isNotNull();

        TaskHistoryDto dlqDto = (TaskHistoryDto) rabbitTemplate.getMessageConverter().fromMessage(deadLetterAmqpMessage);
        assertThat(dlqDto.getTaskId()).isNull();
        assertThat(dlqDto.getAction()).isEqualTo(problematicDto.getAction());


    }

}
