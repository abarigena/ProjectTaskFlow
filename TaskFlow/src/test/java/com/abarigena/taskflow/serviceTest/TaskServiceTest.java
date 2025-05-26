package com.abarigena.taskflow.serviceTest;

import com.abarigena.taskflow.dto.TaskDto;
import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.mapper.TaskMapper;
import com.abarigena.taskflow.serviceNoSQL.TaskHistoryService;
import com.abarigena.taskflow.serviceSQL.TaskServiceImpl;
import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import com.abarigena.taskflow.storeSQL.entity.Task;
import com.abarigena.taskflow.storeSQL.repository.ProjectRepository;
import com.abarigena.taskflow.storeSQL.repository.TaskRepository;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit тесты для TaskServiceImpl")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

/*    @Mock
    private TaskHistoryService taskHistoryService;*/

    @InjectMocks
    private TaskServiceImpl taskService;

    private Long taskId;
    private Task taskEntity;
    private TaskDto taskDto;

    // Новые объекты для createTask
    private Long projectId;
    private Long userId;
    private TaskDto newTaskDto;
    private Task taskEntityToSave;
    private Task savedTaskEntity;
    private TaskDto savedTaskDto;

    //Обьекты для updateTask
    private TaskDto updateTaskDto;
    private Task taskEntityToUpdate;
    private Task updatedTaskEntity;
    private TaskDto updatedTaskDto;

    // Новые заглушки для findTasksByStatusAndPriority
    private Task.Status filterStatus;
    private Task.Priority filterPriority;
    private Pageable filterPageable; // Pageable для запроса
    private Task filteredTask1; // Первая задача, которая должна быть найдена
    private Task filteredTask2; // Вторая задача
    private List<Task> filteredTaskList; // Список Entity, который вернет репозиторий
    private TaskDto filteredTaskDto1; // DTO первой задачи
    private TaskDto filteredTaskDto2; // DTO второй задачи
    private List<TaskDto> filteredTaskDtoList; // Список DTO, который должен вернуть сервис

    @BeforeEach
    void setUp() {
        // данные-заглушки для getTaskById
        taskId = 1L;
        taskEntity = Task.builder()
                .id(taskId)
                .title("Тестовая задача")
                .description("Описание тестовой задачи")
                .status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .projectId(100L)
                .assignedUserId(200L)
                .build();

        taskDto = TaskDto.builder()
                .id(taskId)
                .title("Тестовая задача DTO")
                .description("Описание тестовой задачи DTO")
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH)
                .createdAt(taskEntity.getCreatedAt())
                .updatedAt(taskEntity.getUpdatedAt())
                .projectId(taskEntity.getProjectId())
                .assignedUserId(taskEntity.getAssignedUserId())
                .build();

        // заглушки для сreateTask
        projectId = 100L;
        userId = 200L;
        newTaskDto = TaskDto.builder()
                .title("Новая задача")
                .description("Описание новой задачи")
                .projectId(projectId)
                .assignedUserId(userId) // Назначен пользователь
                .build();

        taskEntityToSave = Task.builder()
                .title("Новая задача")
                .description("Описание новой задачи")
                .projectId(projectId)
                .assignedUserId(userId)
                .status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM)
                .build();

        // Entity, которую репозиторий вернет после сохранения (с сгенерированным ID и установленными временем)
        savedTaskEntity = Task.builder()
                .id(10L) // Сгенерированный ID
                .title("Новая задача")
                .description("Описание новой задачи")
                .projectId(projectId)
                .assignedUserId(userId)
                .status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM)
                .createdAt(LocalDateTime.now().minusSeconds(5)) // Время создания (немного раньше текущего)
                .updatedAt(LocalDateTime.now()) // Время обновления
                .build();

        // DTO, которое маппер создаст из savedTaskEntity и которое должен вернуть сервис
        savedTaskDto = TaskDto.builder()
                .id(savedTaskEntity.getId())
                .title(savedTaskEntity.getTitle())
                .description(savedTaskEntity.getDescription())
                .projectId(savedTaskEntity.getProjectId())
                .assignedUserId(savedTaskEntity.getAssignedUserId())
                .status(savedTaskEntity.getStatus())
                .priority(savedTaskEntity.getPriority())
                .createdAt(savedTaskEntity.getCreatedAt())
                .updatedAt(savedTaskEntity.getUpdatedAt())
                .build();

        // Инициализация данных-заглушек для updateTask
        updateTaskDto = TaskDto.builder()
                .title("Обновленный заголовок")
                .status(Task.Status.IN_PROGRESS)
                .assignedUserId(300L)
                .build();

        taskEntityToUpdate = taskEntity;

        Task taskEntityAfterMapper = Task.builder()
                .id(taskId)
                .title(updateTaskDto.getTitle()) // Обновленный заголовок
                .description(taskEntityToUpdate.getDescription()) // Описание не менялось
                .status(updateTaskDto.getStatus()) // Обновленный статус
                .priority(taskEntityToUpdate.getPriority()) // Приоритет не менялся
                .createdAt(taskEntityToUpdate.getCreatedAt())
                .updatedAt(taskEntityToUpdate.getUpdatedAt()) // Время updatedAt пока старое
                .projectId(taskEntityToUpdate.getProjectId()) // Project ID не менялся
                .assignedUserId(updateTaskDto.getAssignedUserId()) // Обновленный пользователь
                .build();


        // Entity после сохранения (время updatedAt обновлено)
        updatedTaskEntity = Task.builder()
                .id(taskId)
                .title(updateTaskDto.getTitle())
                .description(taskEntityToUpdate.getDescription())
                .status(updateTaskDto.getStatus())
                .priority(taskEntityToUpdate.getPriority())
                .createdAt(taskEntityToUpdate.getCreatedAt())
                .updatedAt(LocalDateTime.now()) // Время updatedAt обновлено
                .projectId(taskEntityToUpdate.getProjectId())
                .assignedUserId(updateTaskDto.getAssignedUserId())
                .build();

        // DTO, возвращаемый после обновления
        updatedTaskDto = TaskDto.builder()
                .id(updatedTaskEntity.getId())
                .title(updatedTaskEntity.getTitle())
                .description(updatedTaskEntity.getDescription())
                .status(updatedTaskEntity.getStatus())
                .priority(updatedTaskEntity.getPriority())
                .createdAt(updatedTaskEntity.getCreatedAt())
                .updatedAt(updatedTaskEntity.getUpdatedAt())
                .projectId(updatedTaskEntity.getProjectId())
                .assignedUserId(updatedTaskEntity.getAssignedUserId())
                .build();

        // ID нового пользователя для проверки
        Long newAssignedUserId = updateTaskDto.getAssignedUserId();
        Long oldAssignedUserId = taskEntityToUpdate.getAssignedUserId(); // 200L

        // ID проекта не меняется в updateTaskDto
        Long newProjectId = taskEntityToUpdate.getProjectId(); // 100L
        Long oldProjectId = taskEntityToUpdate.getProjectId(); // 100L

        // Инициализация данных-заглушек для findTasksByStatusAndPriority
        filterStatus = Task.Status.IN_PROGRESS;
        filterPriority = Task.Priority.HIGH;

        filterPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        filteredTask1 = Task.builder()
                .id(2L)
                .title("Задача в работе")
                .status(filterStatus)
                .priority(filterPriority)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        filteredTask2 = Task.builder()
                .id(3L)
                .title("Приоритетная задача")
                .status(filterStatus)
                .priority(filterPriority)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        filteredTaskList = List.of(filteredTask1, filteredTask2);

        filteredTaskDto1 = TaskDto.builder()
                .id(filteredTask1.getId())
                .title(filteredTask1.getTitle())
                .status(filteredTask1.getStatus())
                .priority(filteredTask1.getPriority())
                .createdAt(filteredTask1.getCreatedAt())
                .build();

        filteredTaskDto2 = TaskDto.builder()
                .id(filteredTask2.getId())
                .title(filteredTask2.getTitle())
                .status(filteredTask2.getStatus())
                .priority(filteredTask2.getPriority())
                .createdAt(filteredTask2.getCreatedAt())
                .build();

        filteredTaskDtoList = List.of(filteredTaskDto1, filteredTaskDto2);
    }

    @Test
    @DisplayName("getTaskById - вернуть успешно задачу по существующему ID")
    void getTaskByIdTest_ShouldReturnTask() {
        Mockito.when(taskRepository.findById(taskId))
                .thenReturn(Mono.just(taskEntity));

        Mockito.when(taskMapper.toDto(any(Task.class)))
                .thenReturn(taskDto);

        Mono<TaskDto> resultMono = taskService.getTaskById(taskId);

        StepVerifier.create(resultMono)
                .expectNext(taskDto)
                .verifyComplete();

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskMapper, times(1)).toDto(taskEntity);
        verifyNoInteractions(projectRepository, userRepository);

    }

    @Test
    @DisplayName("getTaskById - вернуть ошибку ResourceNotFoundException для несуществующего ID")
    void getTaskByIdTest_ShouldReturnTaskNotFoundException() {
        Long nonExistingId = 999L;

        Mockito.when(taskRepository.findById(nonExistingId))
                .thenReturn(Mono.empty());

        Mono<TaskDto> resultMono = taskService.getTaskById(nonExistingId);

        StepVerifier.create(resultMono)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(taskRepository, times(1)).findById(nonExistingId);
        verifyNoInteractions(taskMapper);
        verifyNoInteractions(projectRepository, userRepository);
    }

    @Test
    @DisplayName("createTask - успешно вернуть созданный taskDto, когда и проект и пользователь существует")
    void createTaskTest_ShouldCreateTask() {
        // 1. Маппер: DTO -> Entity для сохранения
        Mockito.when(taskMapper.toEntity(newTaskDto)).thenReturn(taskEntityToSave);

        // 2. Репозиторий проекта: Проверка существования проекта
        when(projectRepository.existsById(projectId)).thenReturn(Mono.just(true));

        // 3. Репозиторий пользователя: Проверка существования назначенного пользователя
        when(userRepository.existsById(userId)).thenReturn(Mono.just(true));

        // 4. Репозиторий задач: Сохранение задачи (возвращаем Entity с ID)
        when(taskRepository.save(taskEntityToSave)).thenReturn(Mono.just(savedTaskEntity));

       /* // 5. Сервис истории: Сохранение истории (возвращаем Mono<Void> или Mono<TaskHistory>)
        when(taskHistoryService.saveHistory(any(TaskHistory.class))).thenReturn(Mono.just(TaskHistory.builder().build()));*/

        // 6. Маппер: Сохраненная Entity -> DTO для возврата
        when(taskMapper.toDto(savedTaskEntity)).thenReturn(savedTaskDto);

        Mono<TaskDto> resultMono = taskService.createTask(newTaskDto);

        StepVerifier.create(resultMono)
                .expectNext(savedTaskDto)
                .verifyComplete();

        verify(taskMapper).toEntity(newTaskDto); // Проверяем, что маппер был вызван для преобразования DTO->Entity
        verify(projectRepository).existsById(projectId); // Проверяем, что проект проверялся по ID
        verify(userRepository).existsById(userId); // Проверяем, что пользователь проверялся по ID
        verify(taskRepository).save(taskEntityToSave); // Проверяем, что задача была сохранена
//        verify(taskHistoryService).saveHistory(any(TaskHistory.class));
        verify(taskMapper).toDto(savedTaskEntity); // Проверяем, что маппер был вызван для преобразования Entity->DTO

        verifyNoMoreInteractions(taskRepository, taskMapper, projectRepository, userRepository);
    }

    @Test
    @DisplayName("createTask - должен успешно создать задачу, когда пользователь не назначен")
    void createTask_UserNotAssigned_ShouldCreateTask() {
        // Создаем DTO без назначенного пользователя
        TaskDto newTaskDtoWithoutUser = TaskDto.builder()
                .title("Новая задача без пользователя")
                .description("Описание новой задачи без пользователя")
                .projectId(projectId)
                .assignedUserId(null) // Пользователь не назначен
                .build();

        // Entity, которую маппер создаст из него
        Task taskEntityToSaveWithoutUser = Task.builder()
                .title("Новая задача без пользователя")
                .description("Описание новой задачи без пользователя")
                .projectId(projectId)
                .assignedUserId(null)
                .status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM)
                .build();

        // Entity, возвращенная репозиторием (с ID)
        Task savedTaskEntityWithoutUser = Task.builder()
                .id(11L) // Другой ID
                .title("Новая задача без пользователя")
                .description("Описание новой задачи без пользователя")
                .projectId(projectId)
                .assignedUserId(null)
                .status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM)
                .createdAt(LocalDateTime.now().minusSeconds(5))
                .updatedAt(LocalDateTime.now())
                .build();

        // DTO для возврата
        TaskDto savedTaskDtoWithoutUser = TaskDto.builder()
                .id(savedTaskEntityWithoutUser.getId())
                .title(savedTaskEntityWithoutUser.getTitle())
                .description(savedTaskEntityWithoutUser.getDescription())
                .projectId(savedTaskEntityWithoutUser.getProjectId())
                .assignedUserId(savedTaskEntityWithoutUser.getAssignedUserId())
                .status(savedTaskEntityWithoutUser.getStatus())
                .priority(savedTaskEntityWithoutUser.getPriority())
                .createdAt(savedTaskEntityWithoutUser.getCreatedAt())
                .updatedAt(savedTaskEntityWithoutUser.getUpdatedAt())
                .build();

        when(taskMapper.toEntity(newTaskDtoWithoutUser)).thenReturn(taskEntityToSaveWithoutUser);
        when(projectRepository.existsById(projectId)).thenReturn(Mono.just(true));
        when(taskRepository.save(taskEntityToSaveWithoutUser)).thenReturn(Mono.just(savedTaskEntityWithoutUser));
//        when(taskHistoryService.saveHistory(any(TaskHistory.class))).thenReturn(Mono.just(TaskHistory.builder().build()));
        when(taskMapper.toDto(savedTaskEntityWithoutUser)).thenReturn(savedTaskDtoWithoutUser);

        Mono<TaskDto> resultMono = taskService.createTask(newTaskDtoWithoutUser);

        StepVerifier.create(resultMono)
                .expectNext(savedTaskDtoWithoutUser)
                .verifyComplete();

        verify(taskMapper).toEntity(newTaskDtoWithoutUser);
        verify(projectRepository).existsById(projectId);
        verify(userRepository, never()).existsById(anyLong());
        verify(taskRepository).save(taskEntityToSaveWithoutUser);
//        verify(taskHistoryService).saveHistory(any(TaskHistory.class));
        verify(taskMapper).toDto(savedTaskEntityWithoutUser);

        verifyNoMoreInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);
    }

    @Test
    @DisplayName("createTask - должен вернуть ошибку ResourceNotFoundException, если проект не найден")
    void createTask_ProjectNotFound_ShouldReturnError() {
        when(taskMapper.toEntity(newTaskDto)).thenReturn(taskEntityToSave);
        when(projectRepository.existsById(projectId)).thenReturn(Mono.just(false));
        when(userRepository.existsById(userId)).thenReturn(Mono.just(true));

        Mono<TaskDto> resultMono = taskService.createTask(newTaskDto);

        StepVerifier.create(resultMono)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(taskMapper).toEntity(newTaskDto);
        verify(projectRepository).existsById(projectId);
        verify(userRepository).existsById(userId);

        verify(taskRepository, never()).save(any());
       /* verify(taskHistoryService, never()).saveHistory(any());*/
        verify(taskMapper, never()).toDto(any());

    }

    @Test
    @DisplayName("updateTask - должен успешно обновить задачу по существующему ID (пользователь назначен)")
    void updateTask_ExistingId_ShouldUpdateTask() {

        Long newAssignedUserId = updateTaskDto.getAssignedUserId(); // 300L
        Long currentProjectId = taskEntityToUpdate.getProjectId(); // 100L (из исходной сущности, не меняется)
        Long taskId = taskEntityToUpdate.getId();

        when(taskRepository.findById(taskId)).thenReturn(Mono.just(taskEntityToUpdate));

        doAnswer(invocation -> {
            // Симулируем, что маппер обновил taskEntityToUpdate
            TaskDto dto = invocation.getArgument(0);
            Task entity = invocation.getArgument(1);
            entity.setTitle(dto.getTitle());
            entity.setStatus(dto.getStatus());
            entity.setAssignedUserId(dto.getAssignedUserId());
            return null;
        }).when(taskMapper).updateEntityFromDto(updateTaskDto, taskEntityToUpdate);

//        when(projectRepository.existsById(currentProjectId)).thenReturn(Mono.just(true));

        when(userRepository.existsById(newAssignedUserId)).thenReturn(Mono.just(true));

        when(taskRepository.save(any(Task.class))).thenReturn(Mono.just(updatedTaskEntity));

//        when(taskHistoryService.saveHistory(any(TaskHistory.class))).thenReturn(Mono.just(TaskHistory.builder().build()));

        when(taskMapper.toDto(any(Task.class))).thenReturn(updatedTaskDto);

        Mono<TaskDto> resultMono = taskService.updateTask(taskId, updateTaskDto);

        StepVerifier.create(resultMono)
                .expectNext(updatedTaskDto)
                .verifyComplete();

        verify(taskRepository).findById(taskId);
        verify(taskMapper).updateEntityFromDto(updateTaskDto, taskEntityToUpdate); // Проверяем, что маппер вызван
//        verify(projectRepository).existsById(currentProjectId);
        verify(userRepository).existsById(newAssignedUserId);
        verify(taskRepository).save(taskEntityToUpdate); // Проверяем, что сохранялась именно измененная Entity
//        verify(taskHistoryService).saveHistory(any(TaskHistory.class)); // Проверяем, что история сохранялась
        verify(taskMapper).toDto(updatedTaskEntity); // Проверяем, что маппер вызван

        verifyNoMoreInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);
    }

    @Test
    @DisplayName("updateTask - должен вернуть ошибку ResourceNotFoundException, если задача для обновления не найдена")
    void updateTask_TaskNotFound_ShouldReturnError() {
        Long nonExistingId = 999L;

        when(taskRepository.findById(nonExistingId)).thenReturn(Mono.empty());

        Mono<TaskDto> resultMono = taskService.updateTask(nonExistingId, updateTaskDto);

        StepVerifier.create(resultMono)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(taskRepository).findById(nonExistingId);
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    @DisplayName("updateTask - должен вернуть ошибку ResourceNotFoundException, если новый назначенный пользователь не найден")
    void updateTask_NewAssignedUserNotFound_ShouldReturnError() {
        Long newAssignedUserId = updateTaskDto.getAssignedUserId(); // 300L
        Long existingProjectId = taskEntityToUpdate.getProjectId();

        when(taskRepository.findById(taskId)).thenReturn(Mono.just(taskEntityToUpdate));

        doAnswer(invocation -> {
            // Симулируем, что маппер обновил taskEntityToUpdate
            TaskDto dto = invocation.getArgument(0);
            Task entity = invocation.getArgument(1);
            entity.setTitle(dto.getTitle());
            entity.setStatus(dto.getStatus());
            entity.setAssignedUserId(dto.getAssignedUserId());
            return null;
        }).when(taskMapper).updateEntityFromDto(updateTaskDto, taskEntityToUpdate);

//        when(projectRepository.existsById(existingProjectId)).thenReturn(Mono.just(true));
        when(userRepository.existsById(newAssignedUserId)).thenReturn(Mono.just(false));

        Mono<TaskDto> resultMono = taskService.updateTask(taskId, updateTaskDto);

        StepVerifier.create(resultMono)
                .expectError(ResourceNotFoundException.class)
                .verify();


        verify(taskRepository).findById(taskId);
        verify(taskMapper).updateEntityFromDto(updateTaskDto, taskEntityToUpdate);
//        verify(projectRepository).existsById(existingProjectId);
        verify(userRepository).existsById(newAssignedUserId);

        verify(taskRepository, never()).save(any(Task.class));
//        verify(taskHistoryService, never()).saveHistory(any(TaskHistory.class));
        verify(taskMapper, never()).toDto(any(Task.class));

        verifyNoMoreInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);
    }

    @Test
    @DisplayName("deleteTask - должен успешно удалить задачу по существующему ID")
    void deleteTask_ExistingId_ShouldDeleteTask() {

        when(taskRepository.findById(taskId)).thenReturn(Mono.just(taskEntity));

        /*when(taskHistoryService.saveHistory(any(TaskHistory.class)))
                .thenReturn(Mono.just(TaskHistory.builder().build()));*/

        when(taskRepository.deleteById(taskId)).thenReturn(Mono.empty());

        Mono<Void> resultMono = taskService.deleteTask(taskId);

        StepVerifier.create(resultMono)
                .verifyComplete();

        verify(taskRepository).findById(taskId);
//        verify(taskHistoryService).saveHistory(any(TaskHistory.class));
        verify(taskRepository).deleteById(taskId);
    }

    @Test
    @DisplayName("deleteTask - должен вернуть ошибку ResourceNotFoundException, если задача для удаления не найдена")
    void deleteTask_TaskNotFound_ShouldReturnError() {
        Long nonExistingId = 999L;

        when(taskRepository.findById(nonExistingId)).thenReturn(Mono.empty());

        Mono<Void> resultMono = taskService.deleteTask(nonExistingId);

        StepVerifier.create(resultMono)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(taskRepository).findById(nonExistingId);
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    @DisplayName("findTasksByStatusAndPriority - должен вернуть задачи с заданным статусом и приоритетом")
    void findTasksByStatusAndPriority_ShouldReturnFilteredTasks() {
        when(taskRepository.findByStatusAndPriority(eq(filterStatus), eq(filterPriority),
                eq(filterPageable)))
                .thenReturn(Flux.fromIterable(filteredTaskList));

        when(taskMapper.toDto(filteredTask1)).thenReturn(filteredTaskDto1);
        when(taskMapper.toDto(filteredTask2)).thenReturn(filteredTaskDto2);

        Flux<TaskDto> resultFlux = taskService.findTaskByStatusAndPriority(filterStatus,
                filterPriority, filterPageable);

        StepVerifier.create(resultFlux)
                .expectNext(filteredTaskDto1)
                .expectNext(filteredTaskDto2)
                .verifyComplete();

        verify(taskRepository).findByStatusAndPriority(eq(filterStatus), eq(filterPriority),
                eq(filterPageable));
        verify(taskMapper).toDto(filteredTask1);
        verify(taskMapper).toDto(filteredTask2);

        verifyNoMoreInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);

    }

    @Test
    @DisplayName("findTasksByStatusAndPriority - должен вернуть ошибку IllegalArgumentException, если статус равен null")
    void findTasksByStatusAndPriority_NullStatus_ShouldReturnError() {
        Flux<TaskDto> resultFlux = taskService.findTaskByStatusAndPriority(null, filterPriority, filterPageable);

        StepVerifier.create(resultFlux)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException && e.getMessage().
                        equals("Статус задачи не может быть null при фильтрации"))
                .verify();

        verifyNoInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);

        verifyNoMoreInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);
    }

    @Test
    @DisplayName("findTasksByStatusAndPriority - должен вернуть ошибку IllegalArgumentException, если приоритет равен null")
    void findTasksByStatusAndPriority_NullPriority_ShouldReturnError() {
        Flux<TaskDto> resultFlux = taskService.findTaskByStatusAndPriority(filterStatus, null, filterPageable);

        StepVerifier.create(resultFlux)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException && e.getMessage().
                        equals("Приоритет задачи не может быть null при фильтрации"))
                .verify();

        verifyNoInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);

        verifyNoMoreInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);
    }

    @Test
    @DisplayName("findTasksByStatusAndPriority - должен вернуть ошибку IllegalArgumentException с правильным сообщением, если оба аргумента null")
    void findTasksByStatusAndPriority_BothNull_ShouldReturnStatusError() {
        Flux<TaskDto> resultFlux = taskService.findTaskByStatusAndPriority(null, null, filterPageable);

        StepVerifier.create(resultFlux)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().equals("Статус задачи не может быть null при фильтрации"))
                .verify();

        verifyNoInteractions(taskRepository, taskMapper, projectRepository, userRepository/*, taskHistoryService*/);
    }

}
