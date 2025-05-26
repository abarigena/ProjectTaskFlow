package com.abarigena.taskflow.storeSQL.repository;

import com.abarigena.taskflow.storeSQL.entity.Task;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TaskRepository extends R2dbcRepository<Task, Long> {

    /**
     * Находит все задачи с поддержкой пагинации и сортировки.
     *
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток всех задач, соответствующих параметрам пагинации.
     */
    Flux<Task> findAllBy(Pageable pageable);

    /**
     * Находит задачи, связанные с указанным проектом, с поддержкой пагинации и сортировки.
     *
     * @param projectId Идентификатор проекта.
     * @param pageable  Параметры пагинации и сортировки.
     * @return Поток задач указанного проекта, соответствующих параметрам пагинации.
     */
    Flux<Task> findByProjectId(Long projectId, Pageable pageable);

    /**
     * Находит задачи, назначенные указанному пользователю, с поддержкой пагинации и сортировки.
     *
     * @param userId   Идентификатор назначенного пользователя.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток задач, назначенных пользователю, соответствующих параметрам пагинации.
     */
    Flux<Task> findByAssignedUserId(Long userId, Pageable pageable);

    /**
     * Находит задачи по указанному статусу и приоритету с поддержкой пагинации и сортировки.
     *
     * @param status   Статус задачи для фильтрации.
     * @param priority Приоритет задачи для фильтрации.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток задач, соответствующих статусу, приоритету и параметрам пагинации.
     */
    Flux<Task> findByStatusAndPriority(Task.Status status, Task.Priority priority, Pageable pageable);
}
