package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.TaskDto;
import com.abarigena.taskflow.dto.TaskHistoryDto;
import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.mapper.TaskHistoryMapper;
import com.abarigena.taskflow.mapper.TaskMapper;
import com.abarigena.taskflow.producer.RabbitProducer;
import com.abarigena.taskflow.serviceNoSQL.TaskHistoryService;
import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import com.abarigena.taskflow.storeSQL.entity.Task;
import com.abarigena.taskflow.storeSQL.repository.ProjectRepository;
import com.abarigena.taskflow.storeSQL.repository.TaskRepository;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskHistoryService taskHistoryService;
    private final RabbitProducer rabbitProducer;
    private final TaskHistoryMapper taskHistoryMapper;

    @Value("${taskflow.routing.notification-topic-delete}")
    private String deleteTopicRoutingKey;

    /**
     * Находит все задачи с поддержкой пагинации и сортировки.
     *
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток всех задач, соответствующих параметрам пагинации, в виде DTO.
     */
    @Override
    public Flux<TaskDto> getAllTasks(Pageable pageable) {
        return taskRepository.findAllBy(pageable)
                .map(taskMapper::toDto);
    }

    /**
     * Находит задачи, связанные с указанным проектом, с поддержкой пагинации и сортировки. Выполняет проверку существования проекта.
     *
     * @param projectId Идентификатор проекта.
     * @param pageable  Параметры пагинации и сортировки.
     * @return Поток задач указанного проекта, соответствующих параметрам пагинации, в виде DTO, или ошибку ResourceNotFoundException, если проект не найден.
     */
    @Override
    public Flux<TaskDto> getTasksByProjectId(Long projectId, Pageable pageable) {

        return projectRepository.findById(projectId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Project", "id", projectId)))
                .flatMapMany(existingProject -> {
                    return taskRepository.findByProjectId(existingProject.getId(), pageable);
                })
                .map(taskMapper::toDto);
    }

    /**
     * Находит задачу по ее идентификатору.
     *
     * @param taskId Идентификатор задачи.
     * @return Mono, содержащий DTO найденной задачи, или ошибку ResourceNotFoundException, если задача не найдена.
     */
    @Override
    public Mono<TaskDto> getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .map(taskMapper::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Task", "id", taskId)));
    }

    /**
     * Создает новую задачу. Выполняет проверки существования связанного проекта и назначенного пользователя (если указан).
     * Автоматически записывает историю создания задачи.
     *
     * @param taskDto DTO задачи для создания.
     * @return Mono, содержащий DTO созданной задачи.
     */
    @Override
    @Transactional
    public Mono<TaskDto> createTask(TaskDto taskDto) {

        Task task = taskMapper.toEntity(taskDto);

        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        if (task.getStatus() == null) {
            task.setStatus(Task.Status.TODO);
        }
        if (task.getPriority() == null) {
            task.setPriority(Task.Priority.MEDIUM);
        }

        Mono<Boolean> projectExists = projectRepository.existsById(task.getProjectId());

        Mono<Boolean> userExists = (task.getAssignedUserId() != null)
                ? userRepository.existsById(task.getAssignedUserId())
                : Mono.just(true);

        return Mono.zip(projectExists, userExists)
                .flatMap(tuple -> {
                    boolean projectFound = tuple.getT1();
                    boolean userFound = tuple.getT2();

                    if (!projectFound) {
                        return Mono.error(new ResourceNotFoundException("Project", "id", task.getProjectId()));
                    }
                    if (!userFound) {
                        return Mono.error(new ResourceNotFoundException("Assigned User", "id", task.getAssignedUserId()));
                    }

                    return taskRepository.save(task);
                })

                .flatMap(savedTask -> {
                    TaskHistory history = TaskHistory.builder()
                            .taskId(savedTask.getId())
                            .action(TaskHistory.Action.CREATE)
                            .performedBy(0L) // TODO получать пользователя из контекста
                            .timestamp(savedTask.getCreatedAt())
                            .status(savedTask.getStatus().name())
                            .details(Map.of("title", savedTask.getTitle()))
                            .build();

                    rabbitProducer.sendCreateNotification(taskHistoryMapper.toDto(history));

                    return taskHistoryService.saveHistory(history)
                            .thenReturn(savedTask);
                })

                .map(taskMapper::toDto);
    }

    /**
     * Обновляет существующую задачу по ее идентификатору. Выполняет проверки существования нового проекта и назначенного пользователя (если изменяются).
     * Автоматически записывает историю обновления задачи.
     *
     * @param id      Идентификатор задачи для обновления.
     * @param taskDto DTO с данными для обновления задачи.
     * @return Mono, содержащий DTO обновленной задачи, или ошибку ResourceNotFoundException, если задача, новый проект или новый пользователь не найдены.
     */
    @Override
    @Transactional
    public Mono<TaskDto> updateTask(Long id, TaskDto taskDto) {

        return taskRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResourceNotFoundException("Task", "id", id))))
                .flatMap(existingTask -> {
                    Long oldProjectId = existingTask.getProjectId();
                    Long oldAssignedUserId = existingTask.getAssignedUserId();

                    taskMapper.updateEntityFromDto(taskDto, existingTask);
                    existingTask.setUpdatedAt(LocalDateTime.now());

                    Long newProjectId = existingTask.getProjectId();
                    Long newAssignedUserId = existingTask.getAssignedUserId();

                    Mono<Void> projectValidationMono = Mono.empty();
                    if (newProjectId == null) {
                        projectValidationMono = Mono.error(new IllegalArgumentException("Project ID не может быть null"));
                    } else if (!newProjectId.equals(oldProjectId)) {
                        projectValidationMono = projectRepository.existsById(newProjectId)
                                .filter(Boolean::booleanValue)
                                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResourceNotFoundException("Project", "id", newProjectId))))
                                .then();
                    }

                    Mono<Void> userValidationMono = Mono.empty();
                    if (!Objects.equals(newAssignedUserId, oldAssignedUserId)) {
                        if (newAssignedUserId != null) {
                            userValidationMono = userRepository.existsById(newAssignedUserId)
                                    .filter(Boolean::booleanValue)
                                    .switchIfEmpty(Mono.defer(() -> Mono.error(new ResourceNotFoundException("Assigned User", "id", newAssignedUserId))))
                                    .then();
                        }
                    }

                    return Mono.when(projectValidationMono, userValidationMono)
                            .then(Mono.defer(() -> taskRepository.save(existingTask)));

                })

                .flatMap(updatedTask -> {
                    TaskHistory history = TaskHistory.builder()
                            .taskId(updatedTask.getId())
                            .action(TaskHistory.Action.UPDATE)
                            .performedBy(0L) // TODO получение пользователя
                            .timestamp(updatedTask.getUpdatedAt())
                            .status(updatedTask.getStatus().name())
                            .details(Map.of("message", "Task fields updated"))
                            .build();

                    rabbitProducer.sendUpdateNotification(taskHistoryMapper.toDto(history));

                    return taskHistoryService.saveHistory(history)
                            .thenReturn(updatedTask);
                })

                .map(taskMapper::toDto);
    }

    /**
     * Удаляет задачу по ее идентификатору. Автоматически записывает историю удаления задачи.
     *
     * @param id Идентификатор задачи для удаления.
     * @return Пустой Mono, сигнализирующий о завершении операции, или ошибку ResourceNotFoundException, если задача не найдена.
     */
    @Override
    @Transactional
    public Mono<Void> deleteTask(Long id) {
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Task", "id", id)))
                .flatMap(existingTask -> {
                    TaskHistory history = TaskHistory.builder()
                            .taskId(existingTask.getId())
                            .action(TaskHistory.Action.DELETE)
                            .performedBy(0L) // TODO: Заменить на реальный ID пользователя
                            .timestamp(LocalDateTime.now())
                            .status("Deleted")
                            .details(Map.of("Title", existingTask.getTitle()))
                            .build();

                    rabbitProducer.sendDeleteNotification(taskHistoryMapper.toDto(history),
                            deleteTopicRoutingKey);

                    return taskHistoryService.saveHistory(history)
                            .then(taskRepository.deleteById(existingTask.getId()));
                });
    }

    /**
     * Находит задачи, назначенные указанному пользователю, с поддержкой пагинации и сортировки.
     *
     * @param userId   Идентификатор назначенного пользователя.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток задач, назначенных пользователю, соответствующих параметрам пагинации, в виде DTO.
     */
    @Override
    public Flux<TaskDto> findByAssignedUserId(Long userId, Pageable pageable) {
        return taskRepository.findByAssignedUserId(userId, pageable)
                .map(taskMapper::toDto);
    }

    /**
     * Находит задачи по указанному статусу и приоритету с поддержкой пагинации и сортировки.
     *
     * @param status   Статус задачи для фильтрации.
     * @param priority Приоритет задачи для фильтрации.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток задач, соответствующих статусу, приоритету и параметрам пагинации, в виде DTO.
     */
    @Override
    public Flux<TaskDto> findTaskByStatusAndPriority(Task.Status status, Task.Priority priority, Pageable pageable) {

        if (status == null) {
            return Flux.error(new IllegalArgumentException("Статус задачи не может быть null при фильтрации"));
        }
        if (priority == null) {
            return Flux.error(new IllegalArgumentException("Приоритет задачи не может быть null при фильтрации"));
        }

        return taskRepository.findByStatusAndPriority(status, priority, pageable)
                .map(taskMapper::toDto);
    }
}
