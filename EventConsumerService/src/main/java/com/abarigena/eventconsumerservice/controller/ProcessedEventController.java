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

    @GetMapping("/processed-events")
    public Flux<DomainEvent> getProcessedEvents() {
        return Flux.fromIterable(kafkaEventConsumer.getProcessedEvents());
    }

    @GetMapping("/errors")
    public Flux<DomainEvent> getErrorEvents() {
        return Flux.fromIterable(kafkaEventConsumer.getDlqEvents());
    }
}
