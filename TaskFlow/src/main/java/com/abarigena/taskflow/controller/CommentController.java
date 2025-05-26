package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.dto.CommentDto;
import com.abarigena.taskflow.serviceSQL.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/{taskId}/comments")
    public Flux<CommentDto> findAllByTaskId(@PathVariable Long taskId,
                                            @RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "size", defaultValue = "10") int size,
                                            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {
        log.info("findAllByProjectId {}", taskId);

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        return commentService.findAllByTaskId(taskId, pageable);
    }

    @PostMapping("/comment")
    public Mono<CommentDto> createComment(@Valid @RequestBody CommentDto commentDto) {
        log.info("createComment {}", commentDto);

        return commentService.createComment(commentDto);
    }

    @PutMapping("/{id}/comment")
    public Mono<CommentDto> updateComment(@PathVariable Long id, @Valid @RequestBody CommentDto commentDto) {
        log.info("updateComment {}", commentDto);

        return commentService.updateComment(id, commentDto);
    }

    @DeleteMapping("/{id}/comment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(@PathVariable Long id) {
        log.info("deleteComment {}", id);

        return commentService.deleteComment(id);
    }

}
