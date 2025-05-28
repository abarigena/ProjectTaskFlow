package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.storeNoSQL.entity.EventLog;
import com.abarigena.taskflow.storeNoSQL.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventLogServiceImpl implements EventLogService {

    private final EventLogRepository eventLogRepository;

    @Override
    public Mono<EventLog> saveEventLog(EventLog eventLog) {
        if (eventLog.getId() == null) {
            eventLog.setId(UUID.randomUUID().toString());
        }
        if (eventLog.getCreatedAt() == null) {
            eventLog.setCreatedAt(LocalDateTime.now());
        }
        log.info("Сохранение EventLog: {}", eventLog);
        return eventLogRepository.save(eventLog)
                .doOnError(error -> log.error("Ошибка сохранения EventLog: {}", error.getMessage(), error));
    }

    @Override
    public Flux<EventLog> getAllEventLogs(Pageable pageable) {
        log.debug("Получение всех EventLog с пагинацией: {}", pageable);
        return eventLogRepository.findAllBy(pageable)
                .doOnError(error -> log.error("Ошибка получения всех EventLog: {}", error.getMessage(), error));
    }

    @Override
    public Mono<EventLog> getEventLogById(String id) {
        log.debug("Получение EventLog по ID: {}", id);
        return eventLogRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("EventLog", "id", id)))
                .doOnError(error -> log.error("Ошибка получения EventLog по ID {}: {}", id, error.getMessage(), error));
    }
}
