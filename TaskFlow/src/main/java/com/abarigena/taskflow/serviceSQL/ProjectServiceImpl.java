package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.ProjectDto;
import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.mapper.ProjectMapper;
import com.abarigena.taskflow.mapper.UserMapper;
import com.abarigena.taskflow.service.ReactiveRedisService;
import com.abarigena.taskflow.service.RedisEventPublisher;
import com.abarigena.taskflow.storeSQL.entity.Project;
import com.abarigena.taskflow.storeSQL.repository.ProjectRepository;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final ReactiveRedisService reactiveRedisService;
    private final RedisEventPublisher redisEventPublisher;

    private static final String PROJECT_ID_CACHE_KEY_PREFIX = "project:id:";
    private static final Duration PROJECT_CACHE_TTL = Duration.ofHours(1);

    /**
     * Находит все проекты с поддержкой пагинации и сортировки.
     *
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток всех проектов, соответствующих параметрам пагинации, в виде DTO.
     */
    @Override
    public Flux<ProjectDto> getAllProjects(Pageable pageable) {
        return projectRepository.findAllBy(pageable)
                .map(projectMapper::toDto);
    }

    /**
     * Находит проекты, принадлежащие указанному владельцу, с поддержкой пагинации и сортировки.
     *
     * @param ownerId  Идентификатор владельца проектов.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток проектов указанного владельца, соответствующих параметрам пагинации, в виде DTO.
     */
    @Override
    public Flux<ProjectDto> getProjectsByOwnerId(Long ownerId, Pageable pageable) {

        return projectRepository.findByOwnerId(ownerId, pageable)
                .map(projectMapper::toDto);
    }

    /**
     * Находит все проекты, в которых участвует указанный пользователь, с поддержкой пагинации и сортировки.
     *
     * @param userId   Идентификатор пользователя.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток проектов, в которых участвует пользователь, соответствующих параметрам пагинации, в виде DTO.
     */
    @Override
    public Flux<ProjectDto> getProjectsByUserId(Long userId, Pageable pageable) {

        return projectRepository.findProjectsByUserId(userId, pageable)
                .map(projectMapper::toDto);
    }

    /**
     * Находит проект по его идентификатору.
     *
     * @param projectId Идентификатор проекта.
     * @return Mono, содержащий DTO найденного проекта, или ошибку ResourceNotFoundException, если проект не найден.
     */
    @Override
    public Mono<ProjectDto> getProjectById(Long projectId) {
        String cacheKey = PROJECT_ID_CACHE_KEY_PREFIX + projectId;
        return reactiveRedisService.getOrSet(
                cacheKey,
                () -> projectRepository.findById(projectId)
                        .map(projectMapper::toDto)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("project", "id", projectId))),
                PROJECT_CACHE_TTL,
                ProjectDto.class
        );
    }

    /**
     * Создает новый проект. Выполняет проверку существования пользователя-владельца.
     *
     * @param projectDto DTO проекта для создания.
     * @return Mono, содержащий DTO созданного проекта.
     */
    @Override
    @Transactional
    public Mono<ProjectDto> createProject(ProjectDto projectDto) {

        Project project = projectMapper.toEntity(projectDto);

        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        return userRepository.findById(project.getOwnerId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Owner", "id", project.getOwnerId())))
                .flatMap(existingOwner -> {
                    return projectRepository.save(project);
                })
                .map(projectMapper::toDto)
                .doOnSuccess(createdProject -> log.info("Создан проект с ID: {}", createdProject.getId()))
                .doOnError(error -> log.error("Ошибка при создании проекта", error));
    }

    /**
     * Обновляет существующий проект по его идентификатору. Выполняет проверку существования нового владельца, если ID владельца изменяется.
     *
     * @param id         Идентификатор проекта для обновления.
     * @param projectDto DTO с данными для обновления проекта.
     * @return Mono, содержащий DTO обновленного проекта, или ошибку ResourceNotFoundException, если проект или новый владелец не найдены.
     */
    @Override
    @Transactional
    public Mono<ProjectDto> updateProject(Long id, ProjectDto projectDto) {
        return projectRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("project", "id", id)))
                .flatMap(existingProject -> {
                    projectMapper.updateEntityFromDto(projectDto, existingProject);
                    existingProject.setUpdatedAt(LocalDateTime.now());
                    Mono<Boolean> ownerCheck = userRepository.existsById(existingProject.getOwnerId())
                            .filter(Boolean::booleanValue)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Owner", "id", existingProject.getOwnerId())));

                    return Mono.when(ownerCheck)
                            .then(Mono.just(existingProject));
                })
                .flatMap(projectRepository::save)
                .flatMap(savedProject ->
                        redisEventPublisher.publishProjectUpdated(savedProject.getId())
                                .thenReturn(savedProject)
                )
                .map(projectMapper::toDto);
    }

    /**
     * Удаляет проект по его идентификатору.
     *
     * @param id Идентификатор проекта для удаления.
     * @return Пустой Mono, сигнализирующий о завершении операции, или ошибку ResourceNotFoundException, если проект не найден.
     */
    @Override
    @Transactional
    public Mono<Void> deleteProject(Long id) {
        return projectRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("project", "id", id)))
                .flatMap(existingProject ->
                        projectRepository.deleteById(existingProject.getId())
                                .then(
                                        redisEventPublisher.publishProjectDeleted(existingProject.getId())
                                )
                );
    }

    /**
     * Добавляет пользователя в проект. Выполняет проверки существования проекта и пользователя.
     *
     * @param projectId Идентификатор проекта.
     * @param userId    Идентификатор пользователя.
     * @return Пустой Mono, сигнализирующий о завершении операции, или ошибку ResourceNotFoundException, если проект или пользователь не найдены.
     */
    @Override
    @Transactional
    public Mono<Void> addUserToProject(Long projectId, Long userId) {

        return projectRepository.findById(projectId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("project", "id", projectId)))
                .flatMap(project ->
                        userRepository.findById(userId)
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", "id", userId)))
                )
                .flatMap(user -> {
                    return projectRepository.addUserToProject(projectId, userId);
                })
                ;
    }

    /**
     * Удаляет пользователя из проекта. Выполняет проверки существования проекта и пользователя.
     *
     * @param projectId Идентификатор проекта.
     * @param userId    Идентификатор пользователя.
     * @return Пустой Mono, сигнализирующий о завершении операции, или ошибку ResourceNotFoundException, если проект или пользователь не найдены.
     */
    @Override
    @Transactional
    public Mono<Void> deleteUserFromProject(Long projectId, Long userId) {

        return projectRepository.findById(projectId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("project", "id", projectId)))
                .flatMap(project ->
                        userRepository.findById(userId)
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", "id", userId)))
                )
                .flatMap(user -> {
                    return projectRepository.deleteUserFromProject(projectId, userId);
                });
    }

    /**
     * Находит всех пользователей, которые являются участниками указанного проекта, в виде DTO.
     * Выполняет проверку существования проекта.
     *
     * @param projectId Идентификатор проекта.
     * @return Поток DTO пользователей, являющихся участниками проекта, или ошибку ResourceNotFoundException, если проект не найден.
     */
    @Override
    public Flux<UserDto> getUsersInProject(Long projectId) {

        return projectRepository.findById(projectId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("project", "id", projectId)))
                .flatMapMany(project -> projectRepository.findUsersInProject(project.getId()))
                .map(userMapper::toDto);

    }
}
