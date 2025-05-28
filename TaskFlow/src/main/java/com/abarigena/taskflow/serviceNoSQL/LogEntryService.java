package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

public interface LogEntryService {

    /**
     * Сохраняет запись журнала.
     * @param logEntry запись журнала для сохранения
     * @return моно сохраненной записи журнала
     */
    Mono<LogEntry> saveLog(LogEntry logEntry);

    /**
     * Получает все записи журнала с использованием пагинации.
     * @param pageable параметры пагинации
     * @return поток записей журнала
     */
    Flux<LogEntry> getAllLogs(Pageable pageable);

    /**
     * Получает записи журнала по указанному уровню логирования с использованием пагинации.
     * @param level уровень логирования
     * @param pageable параметры пагинации
     * @return поток записей журнала
     */
    Flux<LogEntry> getLogsByLevel(LogEntry.LogLevel level, Pageable pageable);

    /**
     * Логирует сообщение об ошибке с дополнительным контекстом.
     * @param message сообщение об ошибке
     * @param context дополнительный контекст
     * @return моно без содержимого
     */
    Mono<Void> logError(String message, Map<String, Object> context);

    /**
     * Логирует информационное сообщение с дополнительным контекстом.
     * @param message информационное сообщение
     * @param context дополнительный контекст
     * @return моно без содержимого
     */
    Mono<Void> logInfo(String message, Map<String, Object> context);

    /**
     * Логирует отладочное сообщение с дополнительным контекстом.
     * @param message отладочное сообщение
     * @param context дополнительный контекст
     * @return моно без содержимого
     */
    Mono<Void> logDebug(String message, Map<String, Object> context);

    /**
     * Логирует предупреждающее сообщение с дополнительным контекстом.
     * @param message предупреждающее сообщение
     * @param context дополнительный контекст
     * @return моно без содержимого
     */
    Mono<Void> logWarn(String message, Map<String, Object> context);
}
