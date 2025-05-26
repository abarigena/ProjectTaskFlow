package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import com.abarigena.taskflow.storeNoSQL.repository.TaskHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskHistoryServiceImpl implements TaskHistoryService {

    private final TaskHistoryRepository taskHistoryRepository;

    /**
     * Сохраняет запись истории задачи в MongoDB. Автоматически устанавливает временную метку, если она не задана.
     *
     * @param history Запись истории задачи для сохранения.
     * @return Mono, содержащий сохраненную запись истории.
     */
    @Override
    public Mono<TaskHistory> saveHistory(TaskHistory history) {

        return taskHistoryRepository.save(history)
                ;
    }

    /**
     * Получает все записи истории изменений для указанной задачи, отсортированные по времени по убыванию (сначала новые).
     *
     * @param taskId Идентификатор задачи (из реляционной БД).
     * @return Поток записей истории для задачи.
     */
    @Override
    public Flux<TaskHistory> getHistoryForTask(Long taskId) {

        return taskHistoryRepository.findByTaskIdOrderByTimestampDesc(taskId)
                ;
    }
}
