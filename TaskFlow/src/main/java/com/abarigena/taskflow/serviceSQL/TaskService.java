package com.abarigena.taskflow.serviceSQL;


import com.abarigena.taskflow.dto.TaskDto;
import com.abarigena.taskflow.storeSQL.entity.Task;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskService {
    Flux<TaskDto> getAllTasks(Pageable pageable);
    Flux<TaskDto> getTasksByProjectId(Long projectId, Pageable pageable);
    Mono<TaskDto> getTaskById(Long taskId);
    Mono<TaskDto> createTask(TaskDto taskDto);
    Mono<TaskDto> updateTask(Long id, TaskDto taskDto);
    Mono<Void> deleteTask(Long id);
    Flux<TaskDto> findByAssignedUserId(Long userId, Pageable pageable);
    Flux<TaskDto> findTaskByStatusAndPriority(Task.Status status, Task.Priority priority,
                                              Pageable pageable);
}
