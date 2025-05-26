package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.dto.ProjectDto;
import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.serviceSQL.ProjectService;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    public Flux<ProjectDto> getAllProjects(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        log.info("Request received for getting all projects. Page: {}, Size: {}, Sort: {}", page, size, sort);

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        return projectService.getAllProjects(pageable);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProjectDto>> getProjectById(@PathVariable Long id) {
        log.info("Request received for getting project with id: {}", id);
        return projectService.getProjectById(id)
                .map(ResponseEntity::ok);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProjectDto> createProject(@Valid @RequestBody ProjectDto projectDto) {
        log.info("Request received for creating project: {}", projectDto.getName());
        return projectService.createProject(projectDto);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProjectDto>> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectDto projectDto) {
        log.info("Request received for updating project with id: {}", id);
        return projectService.updateProject(id, projectDto)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProject(@PathVariable Long id) {
        log.info("Request received for deleting project with id: {}", id);
        return projectService.deleteProject(id);
    }

    @PostMapping("/{projectId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> addUserToProject(@PathVariable Long projectId, @PathVariable Long userId) {
        log.info("Request received to add user {} to project {}", userId, projectId);
        return projectService.addUserToProject(projectId, userId);
    }

    @DeleteMapping("/{projectId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeUserFromProject(@PathVariable Long projectId, @PathVariable Long userId) {
        log.info("Request received to remove user {} from project {}", userId, projectId);
        return projectService.deleteUserFromProject(projectId, userId);
    }

    @GetMapping("/{projectId}/users")
    public Flux<UserDto> getUsersInProject(
            @PathVariable Long projectId
    ) {
        log.info("Request received for getting users in project id: {}", projectId);
        return projectService.getUsersInProject(projectId);
    }

    @GetMapping("/owner/{ownerId}")
    public Flux<ProjectDto> getProjectsByOwnerId(
            @PathVariable Long ownerId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        log.info("Request received for getting projects by owner id: {}, Pageable: {}", ownerId, pageable);

        return projectService.getProjectsByOwnerId(ownerId, pageable);
    }

    @GetMapping("/member/{userId}")
    public Flux<ProjectDto> getProjectsByUserId(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        log.info("Request received for getting projects for user member id: {}, Pageable: {}", userId, pageable);

        return projectService.getProjectsByUserId(userId, pageable);
    }
}
