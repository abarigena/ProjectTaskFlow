package com.abarigena.taskflow.serviceTest;

import com.abarigena.taskflow.serviceNoSQL.TaskHistoryServiceImpl;
import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import com.abarigena.taskflow.storeNoSQL.repository.TaskHistoryRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Модульные тесты для TaskHistoryImpl")
class TaskHistoryServiceTest {
    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @InjectMocks
    private TaskHistoryServiceImpl taskHistoryService;

    private TaskHistory historyEntryToSave;
    private TaskHistory savedHistoryEntry;
    private TaskHistory historyEntry1;
    private TaskHistory historyEntry2;
    private Long taskId;

    @BeforeEach
    void setUp() {
        taskId = 1L;

        historyEntryToSave = TaskHistory.builder()
                .taskId(taskId)
                .action(TaskHistory.Action.CREATE)
                .performedBy(100L)
                .details(Map.of("field", "value"))
                .timestamp(LocalDateTime.now())
                .build();

        savedHistoryEntry = TaskHistory.builder()
                .id(new ObjectId()) // Сгенерированный ID
                .taskId(historyEntryToSave.getTaskId())
                .action(historyEntryToSave.getAction())
                .performedBy(historyEntryToSave.getPerformedBy())
                .details(historyEntryToSave.getDetails())
                .timestamp(historyEntryToSave.getTimestamp())
                .build();

        historyEntry1 = TaskHistory.builder()
                .id(new ObjectId())
                .taskId(taskId)
                .action(TaskHistory.Action.CREATE)
                .timestamp(LocalDateTime.now().minusHours(1))
                .build();
        historyEntry2 = TaskHistory.builder()
                .id(new ObjectId())
                .taskId(taskId)
                .action(TaskHistory.Action.UPDATE)
                .timestamp(LocalDateTime.now().minusMinutes(30))
                .build();
    }

    @Test
    @DisplayName("saveHistory - должен успешно сохранить запись истории")
    void saveHistory_ValidEntry_ShouldSaveAndReturn() {
        when(taskHistoryRepository.save(any(TaskHistory.class)))
                .thenReturn(Mono.just(savedHistoryEntry));

        Mono<TaskHistory> resultMono = taskHistoryService.saveHistory(historyEntryToSave);

        StepVerifier.create(resultMono)
                .expectNext(savedHistoryEntry)
                .verifyComplete();

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryRepository).save(captor.capture());
        TaskHistory captured = captor.getValue();
        assertThat(captured.getTaskId()).isEqualTo(historyEntryToSave.getTaskId());
        assertThat(captured.getAction()).isEqualTo(historyEntryToSave.getAction());

        verifyNoMoreInteractions(taskHistoryRepository);
    }

    @Test
    @DisplayName("saveHistory - должен пробросить ошибку, если репозиторий вернул ошибку")
    void saveHistory_RepositoryError_ShouldPropagateError() {
        RuntimeException dbError = new RuntimeException("Ошибка БД при сохранении истории");
        when(taskHistoryRepository.save(any(TaskHistory.class)))
                .thenReturn(Mono.error(dbError));

        Mono<TaskHistory> resultMono = taskHistoryService.saveHistory(historyEntryToSave);

        StepVerifier.create(resultMono)
                .expectErrorMatches(e -> e instanceof RuntimeException &&
                        e.getMessage().equals(dbError.getMessage()))
                .verify();

        verify(taskHistoryRepository).save(any(TaskHistory.class));
        verifyNoMoreInteractions(taskHistoryRepository);
    }


    @Test
    @DisplayName("getHistoryForTask - должен вернуть Flux с записями истории для задачи")
    void getHistoryForTask_HistoryExists_ShouldReturnFlux() {
        when(taskHistoryRepository.findByTaskIdOrderByTimestampDesc(taskId))
                .thenReturn(Flux.just(historyEntry2, historyEntry1)); // Возвращаем в порядке убывания времени

        Flux<TaskHistory> resultFlux = taskHistoryService.getHistoryForTask(taskId);

        StepVerifier.create(resultFlux)
                .expectNext(historyEntry2)
                .expectNext(historyEntry1)
                .verifyComplete();

        verify(taskHistoryRepository).findByTaskIdOrderByTimestampDesc(taskId);
        verifyNoMoreInteractions(taskHistoryRepository);
    }

    @Test
    @DisplayName("getHistoryForTask - должен вернуть пустой Flux, если история для задачи отсутствует")
    void getHistoryForTask_NoHistory_ShouldReturnEmptyFlux() {
        when(taskHistoryRepository.findByTaskIdOrderByTimestampDesc(taskId))
                .thenReturn(Flux.empty());

        Flux<TaskHistory> resultFlux = taskHistoryService.getHistoryForTask(taskId);

        StepVerifier.create(resultFlux)
                .expectNextCount(0)
                .verifyComplete();

        verify(taskHistoryRepository).findByTaskIdOrderByTimestampDesc(taskId);
        verifyNoMoreInteractions(taskHistoryRepository);
    }

    @Test
    @DisplayName("getHistoryForTask - должен пробросить ошибку, если репозиторий вернул ошибку")
    void getHistoryForTask_RepositoryError_ShouldPropagateError() {
        RuntimeException dbError = new RuntimeException("Ошибка БД при получении истории");
        when(taskHistoryRepository.findByTaskIdOrderByTimestampDesc(taskId))
                .thenReturn(Flux.error(dbError));

        Flux<TaskHistory> resultFlux = taskHistoryService.getHistoryForTask(taskId);

        StepVerifier.create(resultFlux)
                .expectErrorMatches(e -> e instanceof RuntimeException &&
                        e.getMessage().equals(dbError.getMessage()))
                .verify();

        verify(taskHistoryRepository).findByTaskIdOrderByTimestampDesc(taskId); // Пытались получить
        verifyNoMoreInteractions(taskHistoryRepository);
    }

}
