package com.abarigena.taskflow.task15.controller;

import com.abarigena.taskflow.task15.dto.Task15UserDto;
import com.abarigena.taskflow.task15.service.Task15UserService;
import com.abarigena.taskflow.task15.types.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/task15/users")
@RequiredArgsConstructor
@Validated
public class Task15UserController {

    private final Task15UserService userService;

    /**
     * GET /api/task15/users/{id}
     * Получить пользователя по ID
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Task15UserDto>> getUserById(@PathVariable Long id) {
        log.info("GET /api/task15/users/{}", id);
        
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> log.info("Response status: {}", response.getStatusCode()));
    }

    /**
     * GET /api/task15/users
     * Получить всех пользователей
     * Query parameter: ?role=BORROWER (опционально)
     */
    @GetMapping
    public Flux<Task15UserDto> getAllUsers(@RequestParam(required = false) UserRole role) {
        log.info("GET /api/task15/users, role filter: {}", role);
        
        return role != null ? 
            userService.findByRole(role) : 
            userService.findAll();
    }

    /**
     * GET /api/task15/users/search/username/{username}
     * Найти пользователя по username
     */
    @GetMapping("/search/username/{username}")
    public Mono<ResponseEntity<Task15UserDto>> getUserByUsername(@PathVariable String username) {
        log.info("GET /api/task15/users/search/username/{}", username);
        
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/task15/users
     * Создать нового пользователя
     */
    @PostMapping
    public Mono<ResponseEntity<Task15UserDto>> createUser(@Valid @RequestBody Task15UserDto userDto) {
        log.info("POST /api/task15/users, username: {}", userDto.getUsername());
        
        // Проверяем уникальность username и email
        return userService.existsByUsername(userDto.getUsername())
                .flatMap(usernameExists -> {
                    if (usernameExists) {
                        return Mono.error(new RuntimeException("Username already exists"));
                    }
                    return userService.existsByEmail(userDto.getEmail());
                })
                .flatMap(emailExists -> {
                    if (emailExists) {
                        return Mono.error(new RuntimeException("Email already exists"));
                    }
                    return userService.create(userDto);
                })
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .doOnSuccess(response -> log.info("Created user with id: {}", 
                    response.getBody() != null ? response.getBody().getId() : null))
                .doOnError(error -> log.error("Error creating user: {}", error.getMessage()));
    }

    /**
     * PUT /api/task15/users/{id}
     * Обновить пользователя
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Task15UserDto>> updateUser(@PathVariable Long id, 
                                                         @Valid @RequestBody Task15UserDto userDto) {
        log.info("PUT /api/task15/users/{}, username: {}", id, userDto.getUsername());
        
        return userService.update(id, userDto)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build())
                .doOnSuccess(response -> log.info("Update response status: {}", response.getStatusCode()));
    }

    /**
     * DELETE /api/task15/users/{id}
     * Удалить пользователя
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/task15/users/{}", id);
        
        return userService.deleteById(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorReturn(ResponseEntity.notFound().build())
                .doOnSuccess(response -> log.info("Delete response status: {}", response.getStatusCode()));
    }

    /**
     * HEAD /api/task15/users/check/username/{username}
     * Проверить существует ли username
     */
    @RequestMapping(value = "/check/username/{username}", method = RequestMethod.HEAD)
    public Mono<ResponseEntity<Void>> checkUsernameExists(@PathVariable String username) {
        return userService.existsByUsername(username)
                .map(exists -> exists ? 
                    ResponseEntity.ok().<Void>build() : 
                    ResponseEntity.notFound().build());
    }

    /**
     * HEAD /api/task15/users/check/email/{email}
     * Проверить существует ли email
     */
    @RequestMapping(value = "/check/email/{email}", method = RequestMethod.HEAD)
    public Mono<ResponseEntity<Void>> checkEmailExists(@PathVariable String email) {
        return userService.existsByEmail(email)
                .map(exists -> exists ? 
                    ResponseEntity.ok().<Void>build() : 
                    ResponseEntity.notFound().build());
    }
} 