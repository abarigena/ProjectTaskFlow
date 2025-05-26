package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

public interface LogEntryService {

    Mono<LogEntry> saveLog(LogEntry logEntry);

    Flux<LogEntry> getAllLogs(Pageable pageable);

    Flux<LogEntry> getLogsByLevel(LogEntry.LogLevel level, Pageable pageable);

    Mono<Void> logError(String message, Map<String, Object> context);

    Mono<Void> logInfo(String message, Map<String, Object> context);

    Mono<Void> logDebug(String message, Map<String, Object> context);

    Mono<Void> logWarn(String message, Map<String, Object> context);
}
