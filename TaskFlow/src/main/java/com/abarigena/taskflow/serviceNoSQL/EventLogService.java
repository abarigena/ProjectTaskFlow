package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.EventLog;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventLogService {
    /**
     * Сохраняет журнал событий.
     * @param eventLog журнал событий для сохранения
     * @return моно сохраненного журнала событий
     */
    Mono<EventLog> saveEventLog(EventLog eventLog);

    /**
     * Получает все журналы событий с использованием пагинации.
     * @param pageable параметры пагинации
     * @return поток журналов событий
     */
    Flux<EventLog> getAllEventLogs(Pageable pageable);

    /**
     * Получает журнал событий по его идентификатору.
     * @param id идентификатор журнала событий
     * @return моно журнала событий
     */
    Mono<EventLog> getEventLogById(String id);
}