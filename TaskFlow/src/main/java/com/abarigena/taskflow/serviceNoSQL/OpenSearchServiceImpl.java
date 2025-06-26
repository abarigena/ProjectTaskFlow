package com.abarigena.taskflow.serviceNoSQL;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.TermsAggregation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реализация сервиса для работы с OpenSearch
 * Обеспечивает полнотекстовый поиск и аналитику данных
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchServiceImpl implements OpenSearchService {

    private final OpenSearchClient openSearchClient;

    @Override
    public Flux<Map<String, Object>> searchTasks(String query, String status, String priority, Integer from, Integer size) {
        return Mono.fromCallable(() -> {
            try {
                BoolQuery.Builder boolQuery = new BoolQuery.Builder();

                // Полнотекстовый поиск по title и description
                if (query != null && !query.isEmpty()) {
                    boolQuery.should(
                        Query.of(q -> q.match(MatchQuery.of(m -> m.field("title").query(FieldValue.of(query))))),
                        Query.of(q -> q.match(MatchQuery.of(m -> m.field("description").query(FieldValue.of(query)))))
                    ).minimumShouldMatch("1");
                }

                // Фильтр по статусу
                if (status != null && !status.isEmpty()) {
                    boolQuery.filter(Query.of(q -> q.term(TermQuery.of(t -> t.field("status.keyword").value(FieldValue.of(status))))));
                }

                // Фильтр по приоритету
                if (priority != null && !priority.isEmpty()) {
                    boolQuery.filter(Query.of(q -> q.term(TermQuery.of(t -> t.field("priority.keyword").value(FieldValue.of(priority))))));
                }

                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("tasks")
                    .query(Query.of(q -> q.bool(boolQuery.build())))
                    .from(from != null ? from : 0)
                    .size(size != null ? size : 10)
                );

                SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);
                
                List<Map<String, Object>> results = new ArrayList<>();
                HitsMetadata<Map> hits = response.hits();
                if (hits.hits() != null) {
                    for (Hit<Map> hit : hits.hits()) {
                        Map<String, Object> source = hit.source();
                        if (source != null) {
                            source.put("_id", hit.id());
                            source.put("_score", hit.score());
                            results.add(source);
                        }
                    }
                }

                log.info("🔍 Найдено {} задач по запросу: query={}, status={}, priority={}", 
                    results.size(), query, status, priority);
                
                return results;
            } catch (Exception e) {
                log.error("❌ Ошибка поиска задач: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка поиска задач", e);
            }
        }).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<Map<String, Object>> searchComments(String query, Long taskId, Integer from, Integer size) {
        return Mono.fromCallable(() -> {
            try {
                BoolQuery.Builder boolQuery = new BoolQuery.Builder();

                // Полнотекстовый поиск по content
                if (query != null && !query.isEmpty()) {
                    boolQuery.must(Query.of(q -> q.match(MatchQuery.of(m -> m.field("content").query(FieldValue.of(query))))));
                }

                // Фильтр по taskId
                if (taskId != null) {
                    boolQuery.filter(Query.of(q -> q.term(TermQuery.of(t -> t.field("taskId").value(FieldValue.of(taskId))))));
                }

                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("comments")
                    .query(Query.of(q -> q.bool(boolQuery.build())))
                    .from(from != null ? from : 0)
                    .size(size != null ? size : 10)
                );

                SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);
                
                List<Map<String, Object>> results = new ArrayList<>();
                HitsMetadata<Map> hits = response.hits();
                if (hits.hits() != null) {
                    for (Hit<Map> hit : hits.hits()) {
                        Map<String, Object> source = hit.source();
                        if (source != null) {
                            source.put("_id", hit.id());
                            source.put("_score", hit.score());
                            results.add(source);
                        }
                    }
                }

                log.info("🔍 Найдено {} комментариев по запросу: query={}, taskId={}", 
                    results.size(), query, taskId);
                
                return results;
            } catch (Exception e) {
                log.error("❌ Ошибка поиска комментариев: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка поиска комментариев", e);
            }
        }).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<Map<String, Object>> searchProjects(String query, String status, Integer from, Integer size) {
        return Mono.fromCallable(() -> {
            try {
                BoolQuery.Builder boolQuery = new BoolQuery.Builder();

                // Полнотекстовый поиск по name и description
                if (query != null && !query.isEmpty()) {
                    boolQuery.should(
                        Query.of(q -> q.match(MatchQuery.of(m -> m.field("name").query(FieldValue.of(query))))),
                        Query.of(q -> q.match(MatchQuery.of(m -> m.field("description").query(FieldValue.of(query)))))
                    ).minimumShouldMatch("1");
                }

                // Фильтр по статусу
                if (status != null && !status.isEmpty()) {
                    boolQuery.filter(Query.of(q -> q.term(TermQuery.of(t -> t.field("status.keyword").value(FieldValue.of(status))))));
                }

                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("projects")
                    .query(Query.of(q -> q.bool(boolQuery.build())))
                    .from(from != null ? from : 0)
                    .size(size != null ? size : 10)
                );

                SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);
                
                List<Map<String, Object>> results = new ArrayList<>();
                HitsMetadata<Map> hits = response.hits();
                if (hits.hits() != null) {
                    for (Hit<Map> hit : hits.hits()) {
                        Map<String, Object> source = hit.source();
                        if (source != null) {
                            source.put("_id", hit.id());
                            source.put("_score", hit.score());
                            results.add(source);
                        }
                    }
                }

                log.info("🔍 Найдено {} проектов по запросу: query={}, status={}", 
                    results.size(), query, status);
                
                return results;
            } catch (Exception e) {
                log.error("❌ Ошибка поиска проектов: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка поиска проектов", e);
            }
        }).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Map<String, Long>> getTaskAnalyticsByStatus() {
        return Mono.fromCallable(() -> {
            try {
                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("tasks")
                    .size(0) // Не возвращаем документы, только агрегации
                    .aggregations("status_stats", Aggregation.of(a -> a
                        .terms(TermsAggregation.of(t -> t.field("status.keyword")))
                    ))
                );

                SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);
                
                Map<String, Long> stats = new HashMap<>();
                if (response.aggregations() != null) {
                    var statusAgg = response.aggregations().get("status_stats");
                    if (statusAgg != null && statusAgg.isSterms()) {
                        var buckets = statusAgg.sterms().buckets().array();
                        for (var bucket : buckets) {
                            stats.put(bucket.key().toString(), bucket.docCount());
                        }
                    }
                }

                log.info("📊 Статистика задач по статусам: {}", stats);
                return stats;
            } catch (Exception e) {
                log.error("❌ Ошибка получения аналитики задач по статусам: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка получения аналитики", e);
            }
        });
    }

    @Override
    public Mono<Map<String, Long>> getTaskAnalyticsByPriority() {
        return Mono.fromCallable(() -> {
            try {
                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("tasks")
                    .size(0)
                    .aggregations("priority_stats", Aggregation.of(a -> a
                        .terms(TermsAggregation.of(t -> t.field("priority.keyword")))
                    ))
                );

                SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);
                
                Map<String, Long> stats = new HashMap<>();
                if (response.aggregations() != null) {
                    var priorityAgg = response.aggregations().get("priority_stats");
                    if (priorityAgg != null && priorityAgg.isSterms()) {
                        var buckets = priorityAgg.sterms().buckets().array();
                        for (var bucket : buckets) {
                            stats.put(bucket.key().toString(), bucket.docCount());
                        }
                    }
                }

                log.info("📊 Статистика задач по приоритетам: {}", stats);
                return stats;
            } catch (Exception e) {
                log.error("❌ Ошибка получения аналитики задач по приоритетам: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка получения аналитики", e);
            }
        });
    }

    @Override
    public Mono<Map<String, Long>> getProjectAnalyticsByStatus() {
        return Mono.fromCallable(() -> {
            try {
                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("projects")
                    .size(0)
                    .aggregations("status_stats", Aggregation.of(a -> a
                        .terms(TermsAggregation.of(t -> t.field("status.keyword")))
                    ))
                );

                SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);
                
                Map<String, Long> stats = new HashMap<>();
                if (response.aggregations() != null) {
                    var statusAgg = response.aggregations().get("status_stats");
                    if (statusAgg != null && statusAgg.isSterms()) {
                        var buckets = statusAgg.sterms().buckets().array();
                        for (var bucket : buckets) {
                            stats.put(bucket.key().toString(), bucket.docCount());
                        }
                    }
                }

                log.info("📊 Статистика проектов по статусам: {}", stats);
                return stats;
            } catch (Exception e) {
                log.error("❌ Ошибка получения аналитики проектов по статусам: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка получения аналитики", e);
            }
        });
    }

    @Override
    public Mono<Map<String, Long>> getCommentAnalyticsByUser() {
        return Mono.fromCallable(() -> {
            try {
                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("comments")
                    .size(0)
                    .aggregations("user_stats", Aggregation.of(a -> a
                        .terms(TermsAggregation.of(t -> t.field("user_id").size(50)))
                    ))
                );

                SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);
                
                Map<String, Long> stats = new HashMap<>();
                if (response.aggregations() != null) {
                    var userAgg = response.aggregations().get("user_stats");
                    if (userAgg != null && userAgg.isLterms()) {
                        var buckets = userAgg.lterms().buckets().array();
                        for (var bucket : buckets) {
                            stats.put("user_" + bucket.key(), bucket.docCount());
                        }
                    }
                }

                log.info("📊 Статистика комментариев по пользователям: {}", stats);
                return stats;
            } catch (Exception e) {
                log.error("❌ Ошибка получения аналитики комментариев по пользователям: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка получения аналитики", e);
            }
        });
    }

    @Override
    public Mono<String> bulkIndexAllData() {
        return Mono.fromCallable(() -> {
            try {
                // TODO реализовать массовую загрузку данных из PostgreSQL
                log.info("🔄 Начата массовая индексация данных в OpenSearch");
                return "Bulk indexing initiated. Check logs for progress.";
            } catch (Exception e) {
                log.error("❌ Ошибка массовой индексации: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка массовой индексации", e);
            }
        });
    }

    @Override
    public Mono<Map<String, Object>> getClusterHealth() {
        return Mono.fromCallable(() -> {
            try {
                HealthResponse health = openSearchClient.cluster().health(HealthRequest.of(h -> h));
                
                Map<String, Object> healthInfo = new HashMap<>();
                healthInfo.put("status", health.status().jsonValue());
                healthInfo.put("clusterName", health.clusterName());
                healthInfo.put("numberOfNodes", health.numberOfNodes());
                healthInfo.put("numberOfDataNodes", health.numberOfDataNodes());
                healthInfo.put("activePrimaryShards", health.activePrimaryShards());
                healthInfo.put("activeShards", health.activeShards());
                healthInfo.put("relocatingShards", health.relocatingShards());
                healthInfo.put("initializingShards", health.initializingShards());
                healthInfo.put("unassignedShards", health.unassignedShards());
                
                log.info("🟢 Состояние кластера OpenSearch: {}", health.status());
                return healthInfo;
            } catch (Exception e) {
                log.error("❌ Ошибка получения состояния кластера: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка получения состояния кластера", e);
            }
        });
    }

    @Override
    public Flux<Map<String, Object>> searchUsers(String query, Boolean active, Integer from, Integer size) {
        return Mono.fromCallable(() -> {
            try {
                BoolQuery.Builder boolQuery = new BoolQuery.Builder();

                // Полнотекстовый поиск по first_name, last_name, email и fullName
                if (query != null && !query.isEmpty()) {
                    boolQuery.should(
                        Query.of(q -> q.match(MatchQuery.of(m -> m.field("first_name").query(FieldValue.of(query))))),
                        Query.of(q -> q.match(MatchQuery.of(m -> m.field("last_name").query(FieldValue.of(query))))),
                        Query.of(q -> q.match(MatchQuery.of(m -> m.field("email").query(FieldValue.of(query))))),
                        Query.of(q -> q.match(MatchQuery.of(m -> m.field("fullName").query(FieldValue.of(query)))))
                    ).minimumShouldMatch("1");
                }

                // Фильтр по статусу активности
                if (active != null) {
                    boolQuery.filter(Query.of(q -> q.term(TermQuery.of(t -> t.field("active").value(FieldValue.of(active))))));
                }

                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("users")
                    .query(Query.of(q -> q.bool(boolQuery.build())))
                    .from(from != null ? from : 0)
                    .size(size != null ? size : 10)
                );

                SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);
                
                List<Map<String, Object>> results = new ArrayList<>();
                HitsMetadata<Map> hits = response.hits();
                if (hits.hits() != null) {
                    for (Hit<Map> hit : hits.hits()) {
                        Map<String, Object> source = hit.source();
                        if (source != null) {
                            source.put("_id", hit.id());
                            source.put("_score", hit.score());
                            results.add(source);
                        }
                    }
                }

                log.info("🔍 Найдено {} пользователей по запросу: query={}, active={}", 
                    results.size(), query, active);
                
                return results;
            } catch (Exception e) {
                log.error("❌ Ошибка поиска пользователей: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка поиска пользователей", e);
            }
        }).flatMapMany(Flux::fromIterable);
    }
} 