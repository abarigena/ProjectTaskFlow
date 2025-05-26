package com.abarigena.taskflow.storeNoSQL.repository;

import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TaskHistoryRepository extends ReactiveMongoRepository<TaskHistory, ObjectId> {

    /**
     * Находит все записи истории изменений для указанной задачи, отсортированные по времени по убыванию.
     *
     * @param taskId Идентификатор задачи (из реляционной БД).
     * @return Поток записей истории изменений для задачи, отсортированных от новых к старым.
     */
    Flux<TaskHistory> findByTaskIdOrderByTimestampDesc(Long taskId);
}
