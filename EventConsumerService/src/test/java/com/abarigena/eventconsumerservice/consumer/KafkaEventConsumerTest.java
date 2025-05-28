package com.abarigena.eventconsumerservice.consumer;

import com.abarigena.eventconsumerservice.dto.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = { KafkaEventConsumerTest.MAIN_TOPIC, KafkaEventConsumerTest.DLQ_TOPIC },
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093", // Use a different port if 9092 is in use
                "port=9093",
                "auto.create.topics.enable=true"
        }
)
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Reset context and broker for each test
@ActiveProfiles("test")
class KafkaEventConsumerTest {

    static final String MAIN_TOPIC = "test-taskflow-events";
    static final String DLQ_TOPIC = "test-taskflow-events.dlq";
    private static final String TEST_GROUP_ID = "test-consumer-group";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, Object> kafkaProducerTemplate; // For sending messages

    @SpyBean // Use @SpyBean to spy on the actual KafkaEventConsumer instance
    private KafkaEventConsumer kafkaEventConsumerSpy;
    
    // We need to mock the dlqKafkaTemplate inside KafkaEventConsumer
    // Since it's final and injected by @RequiredArgsConstructor, this is tricky.
    // One way is to have a @Primary @Bean in a test configuration for KafkaTemplate
    // and make that one a mock, then ensure the SUT gets this mock for its DLQ template.
    // For this test, we will mock the dlqKafkaTemplate using reflection within the specific test method.
    // A cleaner way in real project might be specific bean qualifiers for dlq template.
    @Autowired
    private KafkaTemplate<String, Object> dlqKafkaTemplateInjected; // This will be the one to mock for DLQ

    @Value("${app.kafka.topics.taskflow-events}")
    private String configuredMainTopic;

    @Value("${app.kafka.topics.taskflow-dlq}")
    private String configuredDlqTopic;


    private KafkaMessageListenerContainer<String, String> dlqListenerContainer;
    private BlockingQueue<ConsumerRecord<String, String>> dlqConsumerRecords;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();


    // Test configuration to provide a potentially mockable KafkaTemplate for DLQ
    @Configuration
    static class TestKafkaConfig {
        // This bean can be mocked using @MockBean in the test class if needed,
        // and then injected into the KafkaEventConsumer if it were designed for such injection.
        // Since KafkaEventConsumer uses @RequiredArgsConstructor, we will use @SpyBean on KafkaEventConsumer
        // and then mock the behavior of its dlqKafkaTemplate field if possible, or the method that uses it.
    }


    @BeforeEach
    void setUp() {
        // Override topic names used by the SUT to match our test topics
        org.springframework.test.util.ReflectionTestUtils.setField(kafkaEventConsumerSpy, "taskflowEventsTopic", MAIN_TOPIC);
        org.springframework.test.util.ReflectionTestUtils.setField(kafkaEventConsumerSpy, "dlqTopic", DLQ_TOPIC);

        kafkaEventConsumerSpy.clearProcessedEvents();
        kafkaEventConsumerSpy.clearDlqEvents();

        // Setup DLQ listener
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(TEST_GROUP_ID + ".dlq", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, String> dlqConsumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        ContainerProperties dlqContainerProperties = new ContainerProperties(DLQ_TOPIC);
        dlqListenerContainer = new KafkaMessageListenerContainer<>(dlqConsumerFactory, dlqContainerProperties);
        dlqConsumerRecords = new LinkedBlockingQueue<>();
        dlqListenerContainer.setupMessageListener((MessageListener<String, String>) record -> dlqConsumerRecords.add(record));
        dlqListenerContainer.start();
        ContainerTestUtils.waitForAssignment(dlqListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic(DLQ_TOPIC));

        // Reset any Mockito interactions on the spy before each test
        Mockito.reset(kafkaEventConsumerSpy.dlqKafkaTemplate); // Assuming dlqKafkaTemplate is accessible; if not, need another way or mock the method
                                                              // Making dlqKafkaTemplate package-private or providing a getter in prod code would help.
                                                              // For now, we'll assume we can mock its behavior via the spy or a helper.
                                                              // The @SpyBean gives us a spy of KafkaEventConsumer.
                                                              // The dlqKafkaTemplate is a field within it.

    }

    @AfterEach
    void tearDown() {
        if (dlqListenerContainer != null) {
            dlqListenerContainer.stop();
        }
    }

    private DomainEvent createSampleEvent(String eventType, String entityType, String entityId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("testData", "value-" + entityId);
        return DomainEvent.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldProcessEventSuccessfullyAndAcknowledge() {
        DomainEvent event = createSampleEvent("NORMAL_PROCESSING", "TASK", "task-001");
        kafkaProducerTemplate.send(MAIN_TOPIC, event.getEntityId(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(kafkaEventConsumerSpy.getProcessedEvents()).hasSize(1);
            assertThat(kafkaEventConsumerSpy.getProcessedEvents().get(0).getEntityId()).isEqualTo("task-001");
        });
        assertThat(kafkaEventConsumerSpy.getDlqEvents()).isEmpty();
        assertThat(dlqConsumerRecords).isEmpty(); // Ensure nothing went to DLQ topic
    }

    @Test
    void shouldSendEventToDlqOnProcessingFailureAndAcknowledgeOriginal() throws Exception {
        DomainEvent eventToFail = createSampleEvent("FORCE_DLQ_TEST", "PROJECT", "project-dlq-002");
        kafkaProducerTemplate.send(MAIN_TOPIC, eventToFail.getEntityId(), eventToFail);

        // Wait for the event to be consumed from DLQ topic
        ConsumerRecord<String, String> dlqReceived = dlqConsumerRecords.poll(15, TimeUnit.SECONDS);
        assertThat(dlqReceived).isNotNull();
        DomainEvent dlqEvent = objectMapper.readValue(dlqReceived.value(), DomainEvent.class);

        assertThat(kafkaEventConsumerSpy.getProcessedEvents()).isEmpty();
        assertThat(kafkaEventConsumerSpy.getDlqEvents()).hasSize(1); // Internal list in SUT
        assertThat(kafkaEventConsumerSpy.getDlqEvents().get(0).getEntityId()).isEqualTo("project-dlq-002");
        
        assertThat(dlqEvent.getEntityId()).isEqualTo("project-dlq-002");
        assertThat(dlqEvent.getEventType()).isEqualTo("FORCE_DLQ_TEST");

        // Further check: ensure the original message is not re-processed by the main listener indefinitely
        // This is implicitly tested by Awaitility timing out if it were stuck in a loop,
        // and by checking processedEvents is empty. A short sleep can help ensure no quick re-processing.
        Thread.sleep(1000); // Wait to see if it gets reprocessed
        assertThat(kafkaEventConsumerSpy.getProcessedEvents()).isEmpty();
    }

    @Test
    void shouldNotAcknowledgeOriginalIfDlqSendFailsEnsuringAtLeastOnce(CapturedOutput output) throws Exception {
        // Get the actual dlqKafkaTemplate instance from the spy
        KafkaTemplate<String, Object> actualDlqKafkaTemplate = (KafkaTemplate<String, Object>) org.springframework.test.util.ReflectionTestUtils.getField(kafkaEventConsumerSpy, "dlqKafkaTemplate");
        KafkaTemplate<String, Object> dlqKafkaTemplateMock = Mockito.spy(actualDlqKafkaTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(kafkaEventConsumerSpy, "dlqKafkaTemplate", dlqKafkaTemplateMock);

        DomainEvent eventToFailDlq = createSampleEvent("FORCE_DLQ_TEST", "COMMENT", "comment-dlq-fail-003");

        // Configure the mocked DLQ KafkaTemplate to throw an exception
        doThrow(new KafkaException("Simulated DLQ Send Failure"))
                .when(dlqKafkaTemplateMock).send(eq(DLQ_TOPIC), anyString(), any(DomainEvent.class));

        // Use an AtomicInteger to count listener invocations
        AtomicInteger listenerInvocationCount = new AtomicInteger(0);
        org.springframework.test.util.ReflectionTestUtils.setField(kafkaEventConsumerSpy, "objectMapper", 
            new ObjectMapper(){ // Wrap to count invocations, bit of a hack
                @Override
                public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                    if (value instanceof DomainEvent && ((DomainEvent)value).getEntityId().equals("comment-dlq-fail-003")) {
                         listenerInvocationCount.incrementAndGet();
                    }
                    return super.writeValueAsString(value);
                }
            }.findAndRegisterModules()
        );


        kafkaProducerTemplate.send(MAIN_TOPIC, eventToFailDlq.getEntityId(), eventToFailDlq);

        // Wait for the listener to be called multiple times (at least twice for re-delivery)
        await().atMost(20, TimeUnit.SECONDS).until(() -> listenerInvocationCount.get() >= 2);
        
        assertThat(kafkaEventConsumerSpy.getProcessedEvents()).isEmpty();
        assertThat(dlqConsumerRecords).isEmpty(); // Nothing should make it to DLQ topic physically

        // Verify logs
        assertThat(output.getOut()).contains("Ошибка обработки события: Тип FORCE_DLQ_TEST ID comment-dlq-fail-003");
        assertThat(output.getOut()).contains("КРИТИЧЕСКАЯ ОШИБКА: Не удалось отправить событие в DLQ топик " + DLQ_TOPIC);
        // Check for multiple "Получено событие" logs for the same event (offset might change on redelivery if not using same container instance)
        // This is harder to assert precisely without more specific log tracing IDs for an event instance.
        // The listenerInvocationCount.get() >= 2 is the primary check for redelivery.
        
        // Ensure the original ObjectMapper is restored if other tests need it pristine
        org.springframework.test.util.ReflectionTestUtils.setField(kafkaEventConsumerSpy, "objectMapper", new ObjectMapper().findAndRegisterModules());
    }
}
