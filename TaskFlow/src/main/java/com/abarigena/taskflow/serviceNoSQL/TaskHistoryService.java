package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskHistoryService {

    Mono<TaskHistory> saveHistory(TaskHistory history);

    Flux<TaskHistory> getHistoryForTask(Long taskId);
}
