package com.abarigena.taskflow.storeNoSQL.repository;

import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface LogEntryRepository extends ReactiveMongoRepository<LogEntry, ObjectId> {

    /**
     * Находит все записи логов с поддержкой пагинации и сортировки.
     *
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток записей логов, соответствующих параметрам пагинации.
     */
    Flux<LogEntry> findAllBy(Pageable pageable);

    /**
     * Находит записи логов указанного уровня с поддержкой пагинации и сортировки.
     *
     * @param level    Уровень лога (Enum).
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток записей логов указанного уровня, соответствующих параметрам пагинации.
     */
    Flux<LogEntry> findByLevel(LogEntry.LogLevel level, Pageable pageable);
}
