package com.abarigena.taskflow.producer;

import com.abarigena.taskflow.dto.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito; // Import Mockito
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import scala.jdk.javaapi.CollectionConverters;
import static org.junit.jupiter.api.Assertions.fail;


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
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" },
        topics = { DomainEventProducerServiceTest.TEST_TOPIC }
)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class DomainEventProducerServiceTest {

    static final String TEST_TOPIC = "test-taskflow-events";

    @Autowired
    private DomainEventProducerService domainEventProducerService;

    @Autowired
    private ApplicationContext applicationContext;

    private EmbeddedKafkaBroker embeddedKafkaBroker;



    private KafkaMessageListenerContainer<String, String> container;
    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Captor
    ArgumentCaptor<DomainEvent> domainEventArgumentCaptor;
    @Captor
    ArgumentCaptor<String> topicArgumentCaptor;
    @Captor
    ArgumentCaptor<String> keyArgumentCaptor;

    @BeforeEach
    void setUp() {
        // Get the EmbeddedKafkaBroker from the ApplicationContext
        embeddedKafkaBroker = applicationContext.getBean(EmbeddedKafkaBroker.class);

        consumerRecords = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties(TEST_TOPIC);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) record -> {
            consumerRecords.add(record);
        });
        container.start();

        ReflectionTestUtils.setField(domainEventProducerService, "taskflowEventsTopic", TEST_TOPIC);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void shouldSendDomainEventSuccessfully(CapturedOutput output) throws Exception {
        Map<String, Object> expectedPayloadMap = Map.of("data", "testData", "value", 123);

        DomainEvent event = DomainEvent.builder()
                .eventType("TEST_EVENT")
                .entityId("entity-123")
                .entityType("TEST_ENTITY")
                .payload(expectedPayloadMap)
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

        Object rawReceivedPayload = receivedEvent.getPayload();
        assertThat(rawReceivedPayload).isNotNull();

        Map<String, Object> actualJavaMapToAssert;

        if (rawReceivedPayload instanceof scala.collection.Map) {
            @SuppressWarnings("unchecked")
            scala.collection.Map<String, Object> scalaMap = (scala.collection.Map<String, Object>) rawReceivedPayload;
            actualJavaMapToAssert = CollectionConverters.asJava(scalaMap);
        }
        else if (rawReceivedPayload instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tempMap = (Map<String, Object>) rawReceivedPayload;
            actualJavaMapToAssert = tempMap;
        }
        else {
            fail("Payload is neither scala.collection.Map nor java.util.Map. Actual type: " + rawReceivedPayload.getClass().getName());
            return;
        }

        assertThat(actualJavaMapToAssert)
                .hasSize(expectedPayloadMap.size())
                .containsAllEntriesOf(expectedPayloadMap);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(output.getOut()).contains("Отправка доменного события в топик '" + TEST_TOPIC + "'");
            assertThat(output.getOut()).contains("Доменное событие успешно отправлено");
            assertThat(output.getOut()).contains("partition=0");
            assertThat(output.getOut()).contains(event.getEntityId());
        });
    }

    @Test
    void shouldHandleErrorWhenKafkaBrokerIsDown(CapturedOutput output) {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, DomainEvent> kafkaTemplateMockForErrorTest = Mockito.mock(KafkaTemplate.class);

        ReflectionTestUtils.setField(domainEventProducerService, "kafkaTemplate", kafkaTemplateMockForErrorTest);

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

        when(kafkaTemplateMockForErrorTest.send(anyString(), anyString(), any(DomainEvent.class))).thenReturn(future);

        domainEventProducerService.sendDomainEvent(event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(kafkaTemplateMockForErrorTest).send(topicArgumentCaptor.capture(), keyArgumentCaptor.capture(), domainEventArgumentCaptor.capture());
            assertThat(topicArgumentCaptor.getValue()).isEqualTo(TEST_TOPIC);
            assertThat(keyArgumentCaptor.getValue()).isEqualTo(event.getEntityId());
            assertThat(domainEventArgumentCaptor.getValue()).isEqualTo(event);

            assertThat(output.getOut()).contains("Отправка доменного события в топик '" + topicArgumentCaptor.getValue() + "'");
            assertThat(output.getOut()).contains("Ошибка при отправке доменного события: Simulated Kafka broker down");
            assertThat(output.getOut()).contains("событие: DomainEvent(eventType=FAIL_EVENT, entityId=entity-fail, entityType=FAIL_ENTITY, payload={data=testData}");
        });

    }

    @Test
    void shouldLogWarningWhenSendingNullEvent(CapturedOutput output) {
        domainEventProducerService.sendDomainEvent(null);
        assertThat(output.getOut()).contains("Попытка отправить null событие. Операция прервана.");
    }
}