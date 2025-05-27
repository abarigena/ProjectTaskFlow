package com.abarigena.taskflow.storeNoSQL.repository;

import com.abarigena.taskflow.storeNoSQL.entity.EventLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends ReactiveMongoRepository<EventLog, String> {
}
