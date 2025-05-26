package com.abarigena.taskflow.storeSQL.repository;

import com.abarigena.taskflow.storeSQL.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CommentRepository extends R2dbcRepository<Comment, Long> {

    /**
     * Находит все комментарии, связанные с указанной задачей, с поддержкой пагинации и сортировки.
     *
     * @param taskId   Идентификатор задачи.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток комментариев для задачи, соответствующих параметрам пагинации.
     */
    Flux<Comment> findByTaskId(Long taskId, Pageable pageable);
}
