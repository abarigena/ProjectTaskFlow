package com.abarigena.taskflow.producer;

import com.abarigena.taskflow.dto.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" },
        topics = { DomainEventProducerServiceTest.TEST_TOPIC }
)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext // Ensures Kafka broker is reset between tests
@ActiveProfiles("test") // Ensure application-test.properties is loaded
class DomainEventProducerServiceTest {

    static final String TEST_TOPIC = "test-taskflow-events";

    @Autowired
    private DomainEventProducerService domainEventProducerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    // Mock KafkaTemplate for failure test
    @MockBean
    private KafkaTemplate<String, DomainEvent> kafkaTemplateMock;


    private KafkaMessageListenerContainer<String, DomainEvent> container;
    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules(); // For payload comparison

    @Captor
    ArgumentCaptor<DomainEvent> domainEventArgumentCaptor;
    @Captor
    ArgumentCaptor<String> topicArgumentCaptor;
    @Captor
    ArgumentCaptor<String> keyArgumentCaptor;


    @BeforeEach
    void setUp() {
        consumerRecords = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        // Use KafkaTestUtils.producerProps for producer properties if needed for a KafkaTemplate for sending directly in tests

        ContainerProperties containerProperties = new ContainerProperties(TEST_TOPIC);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) record -> consumerRecords.add(record));
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void shouldSendDomainEventSuccessfully(CapturedOutput output) throws Exception {
        // Inject the real KafkaTemplate for this specific test
        // Spring will provide the auto-configured one that talks to EmbeddedKafka
        KafkaTemplate<String, DomainEvent> realKafkaTemplate = (KafkaTemplate<String, DomainEvent>) KafkaTestUtils.getPropertyValue(domainEventProducerService, "kafkaTemplate");
        org.springframework.test.util.ReflectionTestUtils.setField(domainEventProducerService, "kafkaTemplate", realKafkaTemplate);
        // Also, set the topic name for the service to use the test topic
        org.springframework.test.util.ReflectionTestUtils.setField(domainEventProducerService, "taskflowEventsTopic", TEST_TOPIC);


        Map<String, Object> payload = Map.of("data", "testData", "value", 123);
        DomainEvent event = DomainEvent.builder()
                .eventType("TEST_EVENT")
                .entityId("entity-123")
                .entityType("TEST_ENTITY")
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();

        domainEventProducerService.sendDomainEvent(event);

        ConsumerRecord<String, String> received = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.topic()).isEqualTo(TEST_TOPIC);
        assertThat(received.key()).isEqualTo("entity-123");

        DomainEvent receivedEvent = objectMapper.readValue(received.value(), DomainEvent.class);
        assertThat(receivedEvent.getEventType()).isEqualTo(event.getEventType());
        assertThat(receivedEvent.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(receivedEvent.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(receivedEvent.getPayload()).isEqualTo(payload); // ObjectMapper handles complex object comparison

        // Check for successful send log
        // Wait a bit for async logging
        Thread.sleep(500); // Not ideal, but simple for this case
        assertThat(output.getOut()).contains("Отправка доменного события в топик '" + TEST_TOPIC + "'");
        assertThat(output.getOut()).contains("Доменное событие успешно отправлено");
        assertThat(output.getOut()).contains("partition=0"); // Assuming 1 partition
        assertThat(output.getOut()).contains(event.getEntityId());
    }

    @Test
    void shouldHandleErrorWhenKafkaBrokerIsDown(CapturedOutput output) throws InterruptedException {
         // Ensure the mock KafkaTemplate is used
        org.springframework.test.util.ReflectionTestUtils.setField(domainEventProducerService, "kafkaTemplate", kafkaTemplateMock);
        org.springframework.test.util.ReflectionTestUtils.setField(domainEventProducerService, "taskflowEventsTopic", "any-topic");


        Map<String, Object> payload = Map.of("data", "testData");
        DomainEvent event = DomainEvent.builder()
                .eventType("FAIL_EVENT")
                .entityId("entity-fail")
                .entityType("FAIL_ENTITY")
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, DomainEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new KafkaException("Simulated Kafka broker down"));

        when(kafkaTemplateMock.send(anyString(), anyString(), any(DomainEvent.class))).thenReturn(future);

        domainEventProducerService.sendDomainEvent(event);

        // Wait for async error logging
        Thread.sleep(500);

        verify(kafkaTemplateMock).send(topicArgumentCaptor.capture(), keyArgumentCaptor.capture(), domainEventArgumentCaptor.capture());
        assertThat(topicArgumentCaptor.getValue()).isEqualTo("any-topic");
        assertThat(keyArgumentCaptor.getValue()).isEqualTo(event.getEntityId());
        assertThat(domainEventArgumentCaptor.getValue()).isEqualTo(event);

        assertThat(output.getOut()).contains("Отправка доменного события в топик 'any-topic'");
        assertThat(output.getOut()).contains("Ошибка при отправке доменного события: Simulated Kafka broker down");
        assertThat(output.getOut()).contains("событие: DomainEvent(eventType=FAIL_EVENT, entityId=entity-fail, entityType=FAIL_ENTITY, payload={data=testData}");
    }

    @Test
    void shouldLogWarningWhenSendingNullEvent(CapturedOutput output) {
        // Ensure the mock KafkaTemplate is used (though it won't be called)
        org.springframework.test.util.ReflectionTestUtils.setField(domainEventProducerService, "kafkaTemplate", kafkaTemplateMock);

        domainEventProducerService.sendDomainEvent(null);

        assertThat(output.getOut()).contains("Попытка отправить null событие. Операция прервана.");
    }
}
