package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskHistoryService {

    /**
     * Сохраняет запись истории задачи.
     * @param history запись истории задачи для сохранения
     * @return моно сохраненной записи истории
     */
    Mono<TaskHistory> saveHistory(TaskHistory history);

    /**
     * Получает историю изменений для указанной задачи.
     * @param taskId идентификатор задачи
     * @return поток истории задачи
     */
    Flux<TaskHistory> getHistoryForTask(Long taskId);
}
