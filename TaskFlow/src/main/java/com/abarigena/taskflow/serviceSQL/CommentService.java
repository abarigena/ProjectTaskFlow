package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.CommentDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommentService {
    Flux<CommentDto> findAllByTaskId(Long projectId, Pageable pageable);

    Mono<CommentDto> createComment(CommentDto commentDto);

    Mono<CommentDto> updateComment(Long id, CommentDto commentDto);

    Mono<Void> deleteComment(Long id);
}
