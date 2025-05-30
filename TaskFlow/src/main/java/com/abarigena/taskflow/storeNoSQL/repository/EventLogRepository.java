package com.abarigena.taskflow.storeNoSQL.repository;

import com.abarigena.taskflow.storeNoSQL.entity.EventLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface EventLogRepository extends ReactiveMongoRepository<EventLog, String> {
    Flux<EventLog> findAllBy(Pageable pageable);
}
