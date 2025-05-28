package com.abarigena.eventconsumerservice.controller;

import com.abarigena.eventconsumerservice.consumer.KafkaEventConsumer;
import com.abarigena.eventconsumerservice.dto.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProcessedEventController {
    private final KafkaEventConsumer kafkaEventConsumer;

    /**
     * Предоставляет эндпоинт для получения списка успешно обработанных доменных событий.
     * @return {@link Flux} с потоком обработанных {@link DomainEvent}.
     */
    @GetMapping("/processed-events")
    public Flux<DomainEvent> getProcessedEvents() {
        return Flux.fromIterable(kafkaEventConsumer.getProcessedEvents());
    }

    /**
     * Предоставляет эндпоинт для получения списка доменных событий, которые не удалось обработать и были отправлены в DLQ.
     * @return {@link Flux} с потоком событий из DLQ.
     */
    @GetMapping("/errors")
    public Flux<DomainEvent> getErrorEvents() {
        return Flux.fromIterable(kafkaEventConsumer.getDlqEvents());
    }
}
