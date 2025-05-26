package com.abarigena.taskflow.storeNoSQL.repository;

import com.abarigena.taskflow.storeNoSQL.entity.DocumentFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DocumentFileRepository extends ReactiveMongoRepository<DocumentFile, ObjectId> {

    /**
     * Находит метаданные всех документов, связанных с указанной задачей.
     *
     * @param taskId Идентификатор задачи (из реляционной БД).
     * @return Поток метаданных документов для задачи.
     */
    Flux<DocumentFile> findByTaskId(Long taskId);
}
