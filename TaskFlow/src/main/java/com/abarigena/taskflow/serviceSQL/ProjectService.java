package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.ProjectDto;
import com.abarigena.taskflow.dto.UserDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectService {
    Flux<ProjectDto> getAllProjects(Pageable pageable);

    Flux<ProjectDto> getProjectsByOwnerId(Long ownerId, Pageable pageable);

    Flux<ProjectDto> getProjectsByUserId(Long userId, Pageable pageable);

    Mono<ProjectDto> getProjectById(Long projectId);

    Mono<ProjectDto> createProject(ProjectDto projectDto);

    Mono<ProjectDto> updateProject(Long id, ProjectDto projectDto);

    Mono<Void> deleteProject(Long id);

    Mono<Void> addUserToProject(Long projectId, Long userId);

    Mono<Void> deleteUserFromProject(Long projectId, Long userId);

    Flux<UserDto> getUsersInProject(Long projectId);
}

