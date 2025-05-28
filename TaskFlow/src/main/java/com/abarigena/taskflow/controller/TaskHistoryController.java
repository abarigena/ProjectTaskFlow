package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.serviceNoSQL.TaskHistoryService;
import com.abarigena.taskflow.serviceSQL.TaskService;
import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tasks/{taskId}/history")
@RequiredArgsConstructor
@Slf4j
public class TaskHistoryController {
    private final TaskHistoryService taskHistoryService;
    private final TaskService taskService;

    /**
     * Получает историю изменений для указанной задачи.
     * @param taskId идентификатор задачи
     * @return поток истории задачи
     */
    @GetMapping
    public Flux<TaskHistory> getHistory(@PathVariable Long taskId) {
        log.info("Getting history for task id {}", taskId);

        return taskService.getTaskById(taskId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Task","id",taskId)))
                .thenMany(taskHistoryService.getHistoryForTask(taskId));
    }
}
