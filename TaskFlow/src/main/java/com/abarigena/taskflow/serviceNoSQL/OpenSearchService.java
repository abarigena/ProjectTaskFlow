package com.abarigena.taskflow.serviceNoSQL;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Сервис для работы с OpenSearch в TaskFlow
 * Обеспечивает поиск по задачам, комментариям, проектам и аналитику
 */
public interface OpenSearchService {

    /**
     * Поиск задач по критериям
     */
    Flux<Map<String, Object>> searchTasks(String query, String status, String priority, Integer from, Integer size);

    /**
     * Поиск комментариев по критериям
     */
    Flux<Map<String, Object>> searchComments(String query, Long taskId, Integer from, Integer size);

    /**
     * Поиск проектов по критериям
     */
    Flux<Map<String, Object>> searchProjects(String query, String status, Integer from, Integer size);

    /**
     * Аналитика задач по статусам
     */
    Mono<Map<String, Long>> getTaskAnalyticsByStatus();

    /**
     * Аналитика задач по приоритетам
     */
    Mono<Map<String, Long>> getTaskAnalyticsByPriority();

    /**
     * Аналитика проектов по статусам
     */
    Mono<Map<String, Long>> getProjectAnalyticsByStatus();

    /**
     * Аналитика комментариев по пользователям
     */
    Mono<Map<String, Long>> getCommentAnalyticsByUser();

    /**
     * Bulk загрузка всех данных из PostgreSQL в OpenSearch
     */
    Mono<String> bulkIndexAllData();

    /**
     * Проверка состояния OpenSearch
     */
    Mono<Map<String, Object>> getClusterHealth();
} 