package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.ProjectDto;
import com.abarigena.taskflow.dto.UserDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectService {
    /**
     * Получает все проекты с использованием пагинации.
     * @param pageable параметры пагинации
     * @return поток DTO проектов
     */
    Flux<ProjectDto> getAllProjects(Pageable pageable);

    /**
     * Получает проекты по идентификатору владельца с использованием пагинации.
     * @param ownerId идентификатор владельца
     * @param pageable параметры пагинации
     * @return поток DTO проектов
     */
    Flux<ProjectDto> getProjectsByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Получает проекты, в которых участвует пользователь, с использованием пагинации.
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return поток DTO проектов
     */
    Flux<ProjectDto> getProjectsByUserId(Long userId, Pageable pageable);

    /**
     * Получает проект по его идентификатору.
     * @param projectId идентификатор проекта
     * @return моно DTO проекта
     */
    Mono<ProjectDto> getProjectById(Long projectId);

    /**
     * Создает новый проект.
     * @param projectDto DTO проекта
     * @return моно DTO созданного проекта
     */
    Mono<ProjectDto> createProject(ProjectDto projectDto);

    /**
     * Обновляет существующий проект.
     * @param id идентификатор проекта
     * @param projectDto DTO проекта с обновленными данными
     * @return моно DTO обновленного проекта
     */
    Mono<ProjectDto> updateProject(Long id, ProjectDto projectDto);

    /**
     * Удаляет проект по его идентификатору.
     * @param id идентификатор проекта
     * @return моно без содержимого
     */
    Mono<Void> deleteProject(Long id);

    /**
     * Добавляет пользователя в проект.
     * @param projectId идентификатор проекта
     * @param userId идентификатор пользователя
     * @return моно без содержимого
     */
    Mono<Void> addUserToProject(Long projectId, Long userId);

    /**
     * Удаляет пользователя из проекта.
     * @param projectId идентификатор проекта
     * @param userId идентификатор пользователя
     * @return моно без содержимого
     */
    Mono<Void> deleteUserFromProject(Long projectId, Long userId);

    /**
     * Получает всех пользователей в указанном проекте.
     * @param projectId идентификатор проекта
     * @return поток DTO пользователей
     */
    Flux<UserDto> getUsersInProject(Long projectId);
}

