package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
import com.abarigena.taskflow.storeNoSQL.repository.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogEntryServiceImpl implements LogEntryService {

    private final LogEntryRepository logEntryRepository;

    /**
     * Сохраняет запись лога в MongoDB. Автоматически устанавливает временную метку, если она не задана.
     * Этот метод используется внутренне другими методами логирования
     *
     * @param logEntry Запись лога для сохранения.
     * @return Mono, содержащий сохраненную запись лога.
     */
    @Override
    public Mono<LogEntry> saveLog(LogEntry logEntry) {
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(LocalDateTime.now());
        }
        return logEntryRepository.save(logEntry)
                .doOnError(error -> System.err.println("FATAL: Could not save log entry to MongoDB: " + error.getMessage()));
    }

    /**
     * Получает все записи логов из MongoDB с поддержкой пагинации и сортировки.
     *
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток записей логов, соответствующих параметрам пагинации.
     */
    @Override
    public Flux<LogEntry> getAllLogs(Pageable pageable) {

        return logEntryRepository.findAllBy(pageable)
                .doOnError(error -> log.error("Error fetching all logs: {}", error.getMessage()));
    }

    /**
     * Получает записи логов указанного уровня из MongoDB с поддержкой пагинации и сортировки.
     *
     * @param level    Уровень лога (Enum).
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток записей логов указанного уровня, соответствующих параметрам пагинации.
     */
    @Override
    public Flux<LogEntry> getLogsByLevel(LogEntry.LogLevel level, Pageable pageable) {

        return logEntryRepository.findByLevel(level, pageable)
                .doOnError(error -> log.error("Error fetching logs by level {}: {}", level, error.getMessage()));
    }

    /**
     * Создает и сохраняет запись лога уровня ERROR.
     *
     * @param message Сообщение лога.
     * @param context Дополнительный контекст в виде Map.
     * @return Пустой Mono, сигнализирующий о завершении операции сохранения.
     */
    @Override
    public Mono<Void> logError(String message, Map<String, Object> context) {
        return recordLog(LogEntry.LogLevel.ERROR, message, context);
    }

    /**
     * Создает и сохраняет запись лога уровня INFO.
     *
     * @param message Сообщение лога.
     * @param context Дополнительный контекст в виде Map.
     * @return Пустой Mono, сигнализирующий о завершении операции сохранения.
     */
    @Override
    public Mono<Void> logInfo(String message, Map<String, Object> context) {
        return recordLog(LogEntry.LogLevel.INFO, message, context);
    }

    /**
     * Создает и сохраняет запись лога уровня DEBUG.
     *
     * @param message Сообщение лога.
     * @param context Дополнительный контекст в виде Map.
     * @return Пустой Mono, сигнализирующий о завершении операции сохранения.
     */
    @Override
    public Mono<Void> logDebug(String message, Map<String, Object> context) {
        return recordLog(LogEntry.LogLevel.DEBUG, message, context);
    }

    /**
     * Создает и сохраняет запись лога уровня WARN.
     *
     * @param message Сообщение лога.
     * @param context Дополнительный контекст в виде Map.
     * @return Пустой Mono, сигнализирующий о завершении операции сохранения.
     */
    @Override
    public Mono<Void> logWarn(String message, Map<String, Object> context) {
        return recordLog(LogEntry.LogLevel.WARN, message, context);
    }

    /**
     * Внутренний вспомогательный метод для создания и сохранения записи лога с указанным уровнем.
     *
     * @param level   Уровень лога.
     * @param message Сообщение лога.
     * @param context Дополнительный контекст.
     * @return Пустой Mono, сигнализирующий о завершении сохранения.
     */
    private Mono<Void> recordLog(LogEntry.LogLevel level, String message, Map<String, Object> context) {
        LogEntry entry = LogEntry.builder()
                .level(level)
                .message(message)
                .context(context == null ? Map.of() : context)
                .timestamp(LocalDateTime.now())
                .build();

        return saveLog(entry).then();
    }
}
