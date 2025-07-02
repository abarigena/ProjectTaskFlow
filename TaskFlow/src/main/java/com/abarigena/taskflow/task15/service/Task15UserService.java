package com.abarigena.taskflow.task15.service;

import com.abarigena.taskflow.task15.dto.Task15UserDto;
import com.abarigena.taskflow.task15.types.UserRole;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Task15UserService {
    
    /**
     * Найти пользователя по ID
     */
    Mono<Task15UserDto> findById(Long id);
    
    /**
     * Найти всех пользователей
     */
    Flux<Task15UserDto> findAll();
    
    /**
     * Найти пользователей по роли
     */
    Flux<Task15UserDto> findByRole(UserRole role);
    
    /**
     * Найти пользователя по username
     */
    Mono<Task15UserDto> findByUsername(String username);
    
    /**
     * Создать нового пользователя
     */
    Mono<Task15UserDto> create(Task15UserDto userDto);
    
    /**
     * Обновить пользователя
     */
    Mono<Task15UserDto> update(Long id, Task15UserDto userDto);
    
    /**
     * Удалить пользователя по ID
     */
    Mono<Void> deleteById(Long id);
    
    /**
     * Проверить существует ли пользователь с данным username
     */
    Mono<Boolean> existsByUsername(String username);
    
    /**
     * Проверить существует ли пользователь с данным email
     */
    Mono<Boolean> existsByEmail(String email);
} 