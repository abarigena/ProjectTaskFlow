package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.dto.TaskDto;
import com.abarigena.taskflow.serviceSQL.TaskService;
import com.abarigena.taskflow.storeSQL.entity.Task;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {
    private final TaskService taskService;

    /**
     * Получает все задачи с использованием пагинации и сортировки.
     * @param page номер страницы
     * @param size количество элементов на странице
     * @param sort параметры сортировки (например, "createdAt,desc")
     * @return поток DTO задач
     */
    @GetMapping
    public Flux<TaskDto> getAllTasks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        log.info("Request received for getting all tasks. Page: {}, Size: {}, Sort: {}", page, size, sort);

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        return taskService.getAllTasks(pageable);
    }

    /**
     * Получает задачу по ее идентификатору.
     * @param id идентификатор задачи
     * @return моно с ResponseEntity, содержащим DTO задачи
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<TaskDto>> getTaskById(@PathVariable Long id) {
        log.info("Request received for getting task with id: {}", id);
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok);
    }

    /**
     * Создает новую задачу.
     * @param taskDto DTO задачи
     * @return моно с ResponseEntity, содержащим DTO созданной задачи
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<TaskDto>> createTask(@Valid @RequestBody TaskDto taskDto) {
        log.info("Request received for creating task: {}", taskDto.getTitle());
        return taskService.createTask(taskDto)
                .map(ResponseEntity::ok);
    }

    /**
     * Обновляет существующую задачу.
     * @param id идентификатор задачи
     * @param taskDto DTO задачи с обновленными данными
     * @return моно с ResponseEntity, содержащим DTO обновленной задачи
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<TaskDto>> updateTask(@PathVariable Long id, @Valid @RequestBody TaskDto taskDto) {
        log.info("Request received for updating task with id: {}", id);

        return taskService.updateTask(id, taskDto)
                .map(ResponseEntity::ok);
    }

    /**
     * Удаляет задачу по ее идентификатору.
     * @param id идентификатор задачи
     * @return моно без содержимого
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTask(@PathVariable Long id) {
        log.info("Request received for deleting task with id: {}", id);
        return taskService.deleteTask(id);
    }

    /**
     * Находит задачи, назначенные указанному пользователю, с использованием пагинации и сортировки.
     * @param userId идентификатор пользователя
     * @param page номер страницы
     * @param size количество элементов на странице
     * @param sort параметры сортировки (например, "createdAt,desc")
     * @return поток DTO задач
     */
    @GetMapping("/assigned/{userId}")
    public Flux<TaskDto> findByAssignedUserId(@PathVariable Long userId,
                                              @RequestParam(value = "page", defaultValue = "0") int page,
                                              @RequestParam(value = "size", defaultValue = "10") int size,
                                              @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {
        log.info("Request received for assigning user with id: {}", userId);

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        return taskService.findByAssignedUserId(userId, pageable);
    }

    /**
     * Получает задачи для указанного проекта с использованием пагинации и сортировки.
     * @param projectId идентификатор проекта
     * @param page номер страницы
     * @param size количество элементов на странице
     * @param sort параметры сортировки (например, "createdAt,desc")
     * @return поток DTO задач
     */
    @GetMapping("/project/{projectId}")
    public Flux<TaskDto> getTasksByProjectId(@PathVariable Long projectId,
                                             @RequestParam(value = "page", defaultValue = "0") int page,
                                             @RequestParam(value = "size", defaultValue = "10") int size,
                                             @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {
        log.info("Request received for getting tasks by project with id: {}", projectId);

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        return taskService.getTasksByProjectId(projectId, pageable);
    }

    /**
     * Получает задачи по статусу и приоритету с использованием пагинации и сортировки.
     * @param status статус задачи
     * @param priority приоритет задачи
     * @param page номер страницы
     * @param size количество элементов на странице
     * @param sort параметры сортировки (например, "createdAt,desc")
     * @return поток DTO задач
     */
    @GetMapping("/find/filter")
    public Flux<TaskDto> getTasksByStatusAndPriority(@RequestParam Task.Status status,
                                                     @RequestParam Task.Priority priority,
                                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                                     @RequestParam(value = "size", defaultValue = "10") int size,
                                                     @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort){
        log.info("Request received for getting tasks by status and priority with id: {}", status);

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        return taskService.findTaskByStatusAndPriority(status, priority, pageable);
    }

}
