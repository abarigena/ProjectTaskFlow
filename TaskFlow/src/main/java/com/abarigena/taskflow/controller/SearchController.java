package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.serviceNoSQL.OpenSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Контроллер для полнотекстового поиска в OpenSearch
 * Реализует API для поиска задач, комментариев и проектов согласно Task-11
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final OpenSearchService openSearchService;

    /**
     * Поиск задач по критериям
     * 
     * @param query текст для поиска (в title и description)
     * @param status статус задачи для фильтрации
     * @param priority приоритет задачи для фильтрации
     * @param from номер начального элемента для пагинации (default: 0)
     * @param size количество элементов на странице (default: 10)
     * @return поток найденных задач
     * 
     * Пример: GET /search/tasks?query=example&status=TODO&priority=HIGH
     */
    @GetMapping("/tasks")
    public Flux<Map<String, Object>> searchTasks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("🔍 Поиск задач: query={}, status={}, priority={}, from={}, size={}", 
            query, status, priority, from, size);
        
        return openSearchService.searchTasks(query, status, priority, from, size);
    }

    /**
     * Поиск комментариев по критериям
     * 
     * @param query текст для поиска в содержимом комментария
     * @param taskId ID задачи для фильтрации комментариев
     * @param from номер начального элемента для пагинации (default: 0)
     * @param size количество элементов на странице (default: 10)
     * @return поток найденных комментариев
     * 
     * Пример: GET /search/comments?query=example&taskId=123
     */
    @GetMapping("/comments")
    public Flux<Map<String, Object>> searchComments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long taskId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("🔍 Поиск комментариев: query={}, taskId={}, from={}, size={}", 
            query, taskId, from, size);
        
        return openSearchService.searchComments(query, taskId, from, size);
    }

    /**
     * Поиск проектов по критериям
     * 
     * @param query текст для поиска (в name и description)
     * @param status статус проекта для фильтрации
     * @param from номер начального элемента для пагинации (default: 0)
     * @param size количество элементов на странице (default: 10)
     * @return поток найденных проектов
     * 
     * Пример: GET /search/projects?query=example
     */
    @GetMapping("/projects")
    public Flux<Map<String, Object>> searchProjects(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("🔍 Поиск проектов: query={}, status={}, from={}, size={}", 
            query, status, from, size);
        
        return openSearchService.searchProjects(query, status, from, size);
    }

    /**
     * Поиск пользователей по критериям
     * 
     * @param query текст для поиска (в first_name, last_name, email, fullName)
     * @param active статус активности пользователя для фильтрации
     * @param from номер начального элемента для пагинации (default: 0)
     * @param size количество элементов на странице (default: 10)
     * @return поток найденных пользователей
     * 
     * Пример: GET /search/users?query=Иван&active=true
     */
    @GetMapping("/users")
    public Flux<Map<String, Object>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("🔍 Поиск пользователей: query={}, active={}, from={}, size={}", 
            query, active, from, size);
        
        return openSearchService.searchUsers(query, active, from, size);
    }
} 