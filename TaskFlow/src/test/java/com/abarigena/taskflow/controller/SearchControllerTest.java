package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.serviceNoSQL.OpenSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Тест для SearchController
 */
@WebFluxTest(SearchController.class)
@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OpenSearchService openSearchService;

    @Test
    void searchTasks_ShouldReturnResults() {
        // Arrange
        Map<String, Object> mockTask = new HashMap<>();
        mockTask.put("id", 1);
        mockTask.put("title", "Test Task");
        mockTask.put("status", "TODO");
        mockTask.put("priority", "HIGH");

        when(openSearchService.searchTasks(any(), any(), any(), any(), any()))
            .thenReturn(Flux.just(mockTask));

        // Act & Assert
        webTestClient.get()
            .uri("/search/tasks?query=test&status=TODO&priority=HIGH")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Map.class)
            .hasSize(1);
    }

    @Test
    void searchComments_ShouldReturnResults() {
        // Arrange
        Map<String, Object> mockComment = new HashMap<>();
        mockComment.put("id", 1);
        mockComment.put("content", "Test comment");
        mockComment.put("taskId", 123);

        when(openSearchService.searchComments(any(), any(), any(), any()))
            .thenReturn(Flux.just(mockComment));

        // Act & Assert
        webTestClient.get()
            .uri("/search/comments?query=test&taskId=123")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Map.class)
            .hasSize(1);
    }

    @Test
    void searchProjects_ShouldReturnResults() {
        // Arrange
        Map<String, Object> mockProject = new HashMap<>();
        mockProject.put("id", 1);
        mockProject.put("name", "Test Project");
        mockProject.put("status", "ACTIVE");

        when(openSearchService.searchProjects(any(), any(), any(), any()))
            .thenReturn(Flux.just(mockProject));

        // Act & Assert
        webTestClient.get()
            .uri("/search/projects?query=test")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Map.class)
            .hasSize(1);
    }
} 