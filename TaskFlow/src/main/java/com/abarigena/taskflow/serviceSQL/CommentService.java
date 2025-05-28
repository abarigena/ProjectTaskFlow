package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.CommentDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommentService {
    /**
     * Находит все комментарии для указанной задачи с использованием пагинации.
     * @param taskId идентификатор задачи
     * @param pageable параметры пагинации
     * @return поток DTO комментариев
     */
    Flux<CommentDto> findAllByTaskId(Long taskId, Pageable pageable);

    /**
     * Создает новый комментарий.
     * @param commentDto DTO комментария
     * @return моно DTO созданного комментария
     */
    Mono<CommentDto> createComment(CommentDto commentDto);

    /**
     * Обновляет существующий комментарий.
     * @param id идентификатор комментария
     * @param commentDto DTO комментария с обновленными данными
     * @return моно DTO обновленного комментария
     */
    Mono<CommentDto> updateComment(Long id, CommentDto commentDto);

    /**
     * Удаляет комментарий по его идентификатору.
     * @param id идентификатор комментария
     * @return моно без содержимого
     */
    Mono<Void> deleteComment(Long id);
}
