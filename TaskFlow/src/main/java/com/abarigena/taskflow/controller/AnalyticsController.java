package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.serviceNoSQL.OpenSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Контроллер для аналитических запросов к OpenSearch
 * Реализует API для получения аналитики по задачам, проектам и комментариям согласно Task-11
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final OpenSearchService openSearchService;

    /**
     * Получить количество задач по статусам
     * 
     * @return статистика задач сгруппированная по статусам
     * 
     * Пример ответа:
     * {
     *   "TODO": 15,
     *   "IN_PROGRESS": 8,
     *   "DONE": 42
     * }
     * 
     * Пример: GET /analytics/tasks/status
     */
    @GetMapping("/tasks/status")
    public Mono<Map<String, Long>> getTaskAnalyticsByStatus() {
        log.info("📊 Запрос аналитики задач по статусам");
        return openSearchService.getTaskAnalyticsByStatus();
    }

    /**
     * Получить количество задач по приоритетам
     * 
     * @return статистика задач сгруппированная по приоритетам
     * 
     * Пример ответа:
     * {
     *   "LOW": 20,
     *   "MEDIUM": 35,
     *   "HIGH": 10
     * }
     * 
     * Пример: GET /analytics/tasks/priority
     */
    @GetMapping("/tasks/priority")
    public Mono<Map<String, Long>> getTaskAnalyticsByPriority() {
        log.info("📊 Запрос аналитики задач по приоритетам");
        return openSearchService.getTaskAnalyticsByPriority();
    }

    /**
     * Получить количество проектов по статусам
     * 
     * @return статистика проектов сгруппированная по статусам
     * 
     * Пример ответа:
     * {
     *   "ACTIVE": 12,
     *   "COMPLETED": 5,
     *   "ARCHIVED": 3
     * }
     * 
     * Пример: GET /analytics/projects/status
     */
    @GetMapping("/projects/status")
    public Mono<Map<String, Long>> getProjectAnalyticsByStatus() {
        log.info("📊 Запрос аналитики проектов по статусам");
        return openSearchService.getProjectAnalyticsByStatus();
    }

    /**
     * Получить количество комментариев по пользователям
     * 
     * @return статистика комментариев сгруппированная по пользователям
     * 
     * Пример ответа:
     * {
     *   "user_1": 25,
     *   "user_2": 18,
     *   "user_3": 32
     * }
     * 
     * Пример: GET /analytics/comments/users
     */
    @GetMapping("/comments/users")
    public Mono<Map<String, Long>> getCommentAnalyticsByUser() {
        log.info("📊 Запрос аналитики комментариев по пользователям");
        return openSearchService.getCommentAnalyticsByUser();
    }

    /**
     * Получить общую информацию о состоянии OpenSearch кластера
     * 
     * @return информация о здоровье кластера
     * 
     * Пример: GET /analytics/cluster/health
     */
    @GetMapping("/cluster/health")
    public Mono<Map<String, Object>> getClusterHealth() {
        log.info("🟢 Запрос состояния кластера OpenSearch");
        return openSearchService.getClusterHealth();
    }

    /**
     * Запустить массовую индексацию всех данных из PostgreSQL в OpenSearch
     * 
     * @return статус операции
     * 
     * Пример: GET /analytics/bulk-index
     */
    @GetMapping("/bulk-index")
    public Mono<String> bulkIndexAllData() {
        log.info("🔄 Запрос массовой индексации данных");
        return openSearchService.bulkIndexAllData();
    }
} 