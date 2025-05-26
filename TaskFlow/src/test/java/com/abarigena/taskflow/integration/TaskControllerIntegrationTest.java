package com.abarigena.taskflow.integration;

import com.abarigena.taskflow.dto.TaskDto;
import com.abarigena.taskflow.dto.TaskHistoryDto;
import com.abarigena.taskflow.storeSQL.entity.Project;
import com.abarigena.taskflow.storeSQL.entity.Task;
import com.abarigena.taskflow.storeSQL.entity.User;
import com.abarigena.taskflow.storeSQL.repository.ProjectRepository;
import com.abarigena.taskflow.storeSQL.repository.TaskRepository;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TaskControllerIntegrationTest.Initializer.class)
@DisplayName("Интеграционные тесты для TaskController (с Testcontainers)")
public class TaskControllerIntegrationTest {

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse(
            "rabbitmq:3-management-alpine"
    )).withExposedPorts(5672, 15672).withAdminUser("rabbitmq").withAdminPassword("rabbitmq");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private Task testTask;
    private Project testProject;
    private User testUser;
    private User anotherUser;
    private Task anotherTaskSameProject;
    private Task taskAnotherProject;
    private Project anotherProject;

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            //postgres
            String host = postgres.getHost();
            Integer port = postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT); // Используем стандартный порт 5432
            String databaseName = postgres.getDatabaseName();

            //r2dbc
            String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s", host, port, databaseName);

            //rabbit
            String rabbitHost = rabbitmq.getHost();
            Integer rabbitPort = rabbitmq.getMappedPort(5672);
            String rabbitUsername = rabbitmq.getAdminUsername();
            String rabbitPassword = rabbitmq.getAdminPassword();

            TestPropertyValues.of(
                    // R2DBC URL (r2dbc:postgresql://host:port/database)
                    "spring.r2dbc.url=" + r2dbcUrl,
                    "spring.r2dbc.username=" + postgres.getUsername(),
                    "spring.r2dbc.password=" + postgres.getPassword(),
                    // Flyway JDBC URL (jdbc:postgresql://host:port/database)
                    "spring.flyway.url=" + postgres.getJdbcUrl(),
                    "spring.flyway.user=" + postgres.getUsername(),
                    "spring.flyway.password=" + postgres.getPassword(),
                    //rabbitMQ
                    "spring.rabbitmq.host=" + rabbitHost,
                    "spring.rabbitmq.port=" + rabbitPort,
                    "spring.rabbitmq.username=" + rabbitUsername,
                    "spring.rabbitmq.password=" + rabbitPassword
            ).applyTo(applicationContext.getEnvironment());
        }
    }

    @BeforeEach
    void setUpDatabase() {
        taskRepository.deleteAll().block();
        projectRepository.deleteAll().block();
        userRepository.deleteAll().block();

        testUser = User.builder()
                .firstName("Тестовый пользователь")
                .lastName("Тестовая фамилия")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .email("тестовый@email.com")
                .build();
        testUser = userRepository.save(testUser).block();

        anotherUser = User.builder()
                .firstName("Другой")
                .lastName("Пользователь")
                .email("another.user@example.com")
                .active(true)
                .build();
        anotherUser = userRepository.save(anotherUser).block();

        testProject = Project.builder()
                .name("Тестовый проект")
                .status(Project.Status.ACTIVE)
                .ownerId(testUser.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testProject = projectRepository.save(testProject).block();

        anotherProject = Project.builder().name("Проект 2").ownerId(testUser.getId()).status(Project.Status.ACTIVE).build();
        anotherProject = projectRepository.save(anotherProject).block();

        testTask = Task.builder()
                .title("Интеграционный тест")
                .description("Задача для интеграционного теста")
                .status(Task.Status.TODO)
                .priority(Task.Priority.HIGH)
                .projectId(testProject.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testTask = taskRepository.save(testTask).block();

        anotherTaskSameProject = Task.builder() // Задача 2 (Проект 1, Исполнитель anotherUser, IN_PROGRESS, HIGH)
                .title("Задача 2 Тест")
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH)
                .projectId(testProject.getId())
                .assignedUserId(anotherUser.getId())
                .build();
        anotherTaskSameProject = taskRepository.save(anotherTaskSameProject).block();

        taskAnotherProject = Task.builder()
                .title("Задача 3 Тест")
                .status(Task.Status.TODO)
                .priority(Task.Priority.LOW)
                .projectId(anotherProject.getId())
                .assignedUserId(testUser.getId())
                .build();
        taskAnotherProject = taskRepository.save(taskAnotherProject).block();
    }

    @AfterEach
    void tearDownDatabase() {
        taskRepository.deleteAll().block();
        projectRepository.deleteAll().block();
        userRepository.deleteAll().block();
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - должен вернуть 200 OK и задачу по существующему ID")
    void getTaskById_ExistingId_ShouldReturnTask() {
        Long existingId = testTask.getId();
        webTestClient.get().uri("/api/tasks/{id}", existingId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(TaskDto.class)
                .value(taskDto -> {
                    org.assertj.core.api.Assertions.assertThat(taskDto.getId()).isEqualTo(existingId);
                    org.assertj.core.api.Assertions.assertThat(taskDto.getTitle()).isEqualTo(testTask.getTitle());
                    org.assertj.core.api.Assertions.assertThat(taskDto.getStatus()).isEqualTo(testTask.getStatus());
                });
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - должен вернуть 404 Not Found для несуществующего ID")
    void getTaskById_NonExistingId_ShouldReturnNotFound() {
        Long nonExistingId = 9999L;
        webTestClient.get().uri("/api/tasks/{id}", nonExistingId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("POST /api/tasks - должен создать задачу и вернуть 201 Created  и отправить уведомление в rabbit")
    void createTask_ValidDto_ShouldReturnCreatedTask() {

        TaskDto newTaskDto = TaskDto.builder()
                .title("Новая интеграционная задача")
                .description("Описание задачи")
                .projectId(testProject.getId())
                .assignedUserId(testUser.getId())
                .build();

        String targetQueue = "task.notifications";

        while (rabbitTemplate.receive(targetQueue) != null) { }

        webTestClient.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(newTaskDto), TaskDto.class)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(TaskDto.class)
                .value(returnedDto -> {
                    assertThat(returnedDto.getId()).isNotNull();
                    assertThat(returnedDto.getTitle()).isEqualTo(newTaskDto.getTitle());
                    assertThat(returnedDto.getDescription()).isEqualTo(newTaskDto.getDescription());
                    assertThat(returnedDto.getProjectId()).isEqualTo(testProject.getId());
                    assertThat(returnedDto.getAssignedUserId()).isEqualTo(testUser.getId());
                    assertThat(returnedDto.getStatus()).isEqualTo(Task.Status.TODO);
                    assertThat(returnedDto.getPriority()).isEqualTo(Task.Priority.MEDIUM);
                    assertThat(returnedDto.getCreatedAt()).isNotNull();
                    assertThat(returnedDto.getUpdatedAt()).isEqualTo(returnedDto.getCreatedAt());

                    Object receivedMessage = rabbitTemplate.receiveAndConvert(targetQueue, 5000);

                    assertThat(receivedMessage)
                            .as("Сообщение в очереди '%s'", targetQueue)
                            .isNotNull()
                            .isInstanceOf(TaskHistoryDto.class);

                    Task savedTask = taskRepository.findById(returnedDto.getId()).block();
                    assertThat(savedTask).isNotNull();
                    assertThat(savedTask.getTitle()).isNotNull();
                    assertThat(savedTask.getTitle()).isEqualTo(newTaskDto.getTitle());
                });
    }

    @Test
    @DisplayName("POST /api/tasks - должен вернуть 400 Bad Request при невалидном DTO (пустой title)")
    void createTask_InvalidDto_ShouldReturnBadRequest() {
        TaskDto invalidTaskDto = TaskDto.builder()
                .title("")
                .description("Описание")
                .projectId(testProject.getId())
                .build();

        webTestClient.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(invalidTaskDto), TaskDto.class)
                .exchange()

                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.details").isNotEmpty()
                .jsonPath("$.details.title").isEqualTo("Название не должно быть пустым");
    }

    @Test
    @DisplayName("POST /api/tasks - должен вернуть 400 Bad Request при невалидном DTO (null projectId)")
    void createTask_NullProjectId_ShouldReturnBadRequest() {
        TaskDto invalidTaskDto = TaskDto.builder()
                .title("Задача с null проектом")
                .description("Описание")
                .projectId(null)
                .build();

        webTestClient.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(invalidTaskDto), TaskDto.class)
                .exchange()

                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.details.projectId").isEqualTo("Id проекта не должно быть пустым");
    }

    @Test
    @DisplayName("POST /api/tasks - должен вернуть 404 Not Found при несуществующем projectId")
    void createTask_NonExistingProjectId_ShouldReturnNotFound() {
        Long nonExistingProjectId = 9999L;

        TaskDto taskWithNonExistingProject = TaskDto.builder()
                .title("Задача с несущ. проектом")
                .description("Описание")
                .projectId(nonExistingProjectId)
                .build();

        webTestClient.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(taskWithNonExistingProject), TaskDto.class)
                .exchange()

                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.message").isEqualTo(String.format("Project not found with id: '%d'", nonExistingProjectId));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - должен успешно обновить задачу и отправить сообщение в rabbit")
    void updateTask_ValidDtoAndExistingId_ShouldReturnUpdatedTask() {
        Long existingId = testTask.getId();

        TaskDto updateDto = TaskDto.builder()
                .title("Обновленный Заголовок Задачи")
                .status(Task.Status.IN_PROGRESS) // Меняем статус
                .assignedUserId(anotherUser.getId()) // Меняем исполнителя
                .projectId(testProject.getId()) // Нужно передать projectId, т.к. он @NotNull
                .priority(testTask.getPriority()) // Нужно передать priority, т.к. он @NotNull
                .build();

        String targetQueue = "task.audit.fanout";

        while (rabbitTemplate.receive(targetQueue) != null) {}

        webTestClient.put().uri("/api/tasks/{id}", existingId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), TaskDto.class)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(TaskDto.class)
                .value(returnedDto -> {
                    assertThat(returnedDto.getId()).isEqualTo(existingId);
                    assertThat(returnedDto.getTitle()).isEqualTo(updateDto.getTitle()); // Проверяем обновленный title
                    assertThat(returnedDto.getStatus()).isEqualTo(updateDto.getStatus()); // Проверяем обновленный статус
                    assertThat(returnedDto.getAssignedUserId()).isEqualTo(updateDto.getAssignedUserId()); // Проверяем обновленного исполнителя
                    assertThat(returnedDto.getProjectId()).isEqualTo(testProject.getId()); // Проект не менялся
                    assertThat(returnedDto.getDescription()).isEqualTo(testTask.getDescription()); // Описание не менялось
                    assertThat(returnedDto.getPriority()).isEqualTo(testTask.getPriority()); // Приоритет не менялся
                    assertThat(returnedDto.getUpdatedAt()).isAfter(testTask.getUpdatedAt()); // Время обновления должно измениться

                    Object receivedMessage = rabbitTemplate.receiveAndConvert(targetQueue, 5000);

                    assertThat(receivedMessage)
                            .as("Сообщение в очереди '%s'", targetQueue)
                            .isNotNull()
                            .isInstanceOf(TaskHistoryDto.class);

                    Task updatedTaskInDb = taskRepository.findById(existingId).block();
                    assertThat(updatedTaskInDb).isNotNull();
                    assertThat(updatedTaskInDb.getTitle()).isEqualTo(updateDto.getTitle());
                    assertThat(updatedTaskInDb.getStatus()).isEqualTo(updateDto.getStatus());
                    assertThat(updatedTaskInDb.getAssignedUserId()).isEqualTo(updateDto.getAssignedUserId());
                    assertThat(updatedTaskInDb.getUpdatedAt()).isAfter(testTask.getUpdatedAt());
                });
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - должен вернуть 404 Not Found для несуществующего ID")
    void updateTask_NonExistingId_ShouldReturnNotFound() {
        Long nonExistingId = 9999L;

        TaskDto updateDto = TaskDto.builder()
                .title("Попытка обновить")
                .projectId(testProject.getId())
                .priority(Task.Priority.LOW)
                .status(Task.Status.DONE)
                .build();

        webTestClient.put().uri("/api/tasks/{id}", nonExistingId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), TaskDto.class)
                .exchange()

                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - должен вернуть 400 Bad Request при невалидном DTO")
    void updateTask_InvalidDto_ShouldReturnBadRequest() {
        Long existingId = testTask.getId();

        TaskDto updateDto = TaskDto.builder()
                .title(" ") // Пустой title
                .projectId(testProject.getId())
                .priority(Task.Priority.LOW)
                .status(Task.Status.DONE)
                .build();

        webTestClient.put().uri("/api/tasks/{id}", existingId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), TaskDto.class)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.details.title").isEqualTo("Название не должно быть пустым");
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - должен вернуть 404 Not Found при попытке установить несуществующий projectId")
    void updateTask_NonExistingProjectId_ShouldReturnNotFound() {
        Long existingId = testTask.getId();
        Long nonExistingProjectId = 9999L;

        TaskDto updateDto = TaskDto.builder()
                .title("Смена на несущ. проект")
                .projectId(nonExistingProjectId)
                .priority(testTask.getPriority())
                .status(testTask.getStatus())
                .build();

        webTestClient.put().uri("/api/tasks/{id}", existingId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateDto), TaskDto.class)
                .exchange()

                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(String.format("Project not found with id: '%d'", nonExistingProjectId));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - должен удалить задачу и вернуть 204 No Content и отправить сообщение в rabbit")
    void deleteTask_ExistingId_ShouldReturnNoContent() {
        Long existingId = testTask.getId();

        String targetQueue = "task.notifications.topic";

        while (rabbitTemplate.receive(targetQueue) != null) {}

        webTestClient.delete().uri("/api/tasks/{id}", existingId)
                .exchange()

                .expectStatus().isNoContent()
                ;

        Mono<Task> findMono = taskRepository.findById(existingId);
        StepVerifier.create(findMono)
                .expectNextCount(0)
                .verifyComplete();

        Object receivedMessage = rabbitTemplate.receiveAndConvert(targetQueue, 5000);

        assertThat(receivedMessage)
                .as("Сообщение в очереди '%s'", targetQueue)
                .isNotNull()
                .isInstanceOf(TaskHistoryDto.class);
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - должен вернуть 404 Not Found для несуществующего ID")
    void deleteTask_NonExistingId_ShouldReturnNotFound() {
        Long nonExistingId = 9999L;

        webTestClient.delete().uri("/api/tasks/{id}", nonExistingId)
                .exchange()

                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/tasks/assigned/{userId} - должен вернуть задачи, назначенные пользователю")
    void findByAssignedUserId_ShouldReturnAssignedTasks() {
        webTestClient.get().uri("/api/tasks/assigned/{userId}", testUser.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(TaskDto.class)
                .hasSize(1);

        webTestClient.get().uri("/api/tasks/assigned/{userId}", anotherUser.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(TaskDto.class)
                .hasSize(1)
                .value(tasks -> {
                    assertThat(tasks.get(0).getId()).isEqualTo(anotherTaskSameProject.getId());
                });

        User userWithoutTasks = userRepository.save(User.builder().email("no.tasks@example.com").firstName("нетутаски")
                .lastName("нетутаскин").build()).block();
        webTestClient.get().uri("/api/tasks/assigned/{userId}", userWithoutTasks.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(TaskDto.class)
                .hasSize(0);
    }

    @Test
    @DisplayName("GET /api/tasks/project/{projectId} - должен вернуть задачи проекта")
    void getTasksByProjectId_ShouldReturnProjectTasks() {
        webTestClient.get().uri("/api/tasks/project/{projectId}", testProject.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(TaskDto.class)
                .hasSize(2);

        webTestClient.get().uri("/api/tasks/project/{projectId}", anotherProject.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(TaskDto.class)
                .hasSize(1)
                .value(tasks -> {
                    assertThat(tasks.get(0).getId()).isEqualTo(taskAnotherProject.getId());
                });

        Long nonExistingProjectId = 9999L;
        webTestClient.get().uri("/api/tasks/project/{projectId}", nonExistingProjectId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/tasks/find/filter - должен вернуть задачи по статусу и приоритету")
    void getTasksByStatusAndPriority_ShouldReturnFilteredTasks() {
        Task.Status status = Task.Status.TODO;
        Task.Priority priority = Task.Priority.HIGH;

        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/tasks/find/filter")
                        .queryParam("status", status.name())
                        .queryParam("priority", priority.name())
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(TaskDto.class)
                .hasSize(1)
                .value(tasks -> {
                    assertThat(tasks.get(0).getId()).isEqualTo(testTask.getId());
                    assertThat(tasks.get(0).getStatus()).isEqualTo(status);
                    assertThat(tasks.get(0).getPriority()).isEqualTo(priority);
                });
    }

    @Test
    @DisplayName("GET /api/tasks/find/filter - должен вернуть пустой список, если нет совпадений")
    void getTasksByStatusAndPriority_NoMatches_ShouldReturnEmptyList() {
        Task.Status status = Task.Status.DONE;
        Task.Priority priority = Task.Priority.HIGH;

        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/tasks/find/filter")
                        .queryParam("status", status.name())
                        .queryParam("priority", priority.name())
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(TaskDto.class)
                .hasSize(0);
    }

    @Test
    @DisplayName("GET /api/tasks/find/filter - должен вернуть 400 Bad Request, если параметры не указаны или невалидны")
    void getTasksByStatusAndPriority_MissingParams_ShouldReturnBadRequest() {
        webTestClient.get().uri("/api/tasks/find/filter")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/api/tasks/find/filter")
                        .queryParam("status", "INVALID_STATUS")
                        .queryParam("priority", Task.Priority.HIGH.name())
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();

    }
}
