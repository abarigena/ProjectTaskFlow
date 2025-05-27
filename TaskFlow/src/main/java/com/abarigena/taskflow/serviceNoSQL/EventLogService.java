package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.EventLog;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventLogService {
    Mono<EventLog> saveEventLog(EventLog eventLog);
    Flux<EventLog> getAllEventLogs(Pageable pageable);
    Mono<EventLog> getEventLogById(String id);
}