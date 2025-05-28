package com.abarigena.taskflow.serviceSQL;


import com.abarigena.taskflow.dto.TaskDto;
import com.abarigena.taskflow.storeSQL.entity.Task;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskService {
    /**
     * Получает все задачи с использованием пагинации.
     * @param pageable параметры пагинации
     * @return поток DTO задач
     */
    Flux<TaskDto> getAllTasks(Pageable pageable);

    /**
     * Получает задачи для указанного проекта с использованием пагинации.
     * @param projectId идентификатор проекта
     * @param pageable параметры пагинации
     * @return поток DTO задач
     */
    Flux<TaskDto> getTasksByProjectId(Long projectId, Pageable pageable);

    /**
     * Получает задачу по ее идентификатору.
     * @param taskId идентификатор задачи
     * @return моно DTO задачи
     */
    Mono<TaskDto> getTaskById(Long taskId);

    /**
     * Создает новую задачу.
     * @param taskDto DTO задачи
     * @return моно DTO созданной задачи
     */
    Mono<TaskDto> createTask(TaskDto taskDto);

    /**
     * Обновляет существующую задачу.
     * @param id идентификатор задачи
     * @param taskDto DTO задачи с обновленными данными
     * @return моно DTO обновленной задачи
     */
    Mono<TaskDto> updateTask(Long id, TaskDto taskDto);

    /**
     * Удаляет задачу по ее идентификатору.
     * @param id идентификатор задачи
     * @return моно без содержимого
     */
    Mono<Void> deleteTask(Long id);

    /**
     * Находит задачи, назначенные указанному пользователю, с использованием пагинации.
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return поток DTO задач
     */
    Flux<TaskDto> findByAssignedUserId(Long userId, Pageable pageable);

    /**
     * Находит задачи по указанному статусу и приоритету с использованием пагинации.
     * @param status статус задачи
     * @param priority приоритет задачи
     * @param pageable параметры пагинации
     * @return поток DTO задач
     */
    Flux<TaskDto> findTaskByStatusAndPriority(Task.Status status, Task.Priority priority,
                                              Pageable pageable);
}
