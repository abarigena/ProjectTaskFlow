package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.CommentDto;
import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.mapper.CommentMapper;
import com.abarigena.taskflow.storeSQL.entity.Comment;
import com.abarigena.taskflow.storeSQL.repository.CommentRepository;
import com.abarigena.taskflow.storeSQL.repository.TaskRepository;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    /**
     * Находит все комментарии, связанные с указанной задачей, с поддержкой пагинации и сортировки.
     *
     * @param taskId   Идентификатор задачи, для которой ищутся комментарии.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток комментариев для задачи, соответствующих параметрам пагинации, в виде DTO.
     */
    @Override
    public Flux<CommentDto> findAllByTaskId(Long taskId, Pageable pageable) {
        return commentRepository.findByTaskId(taskId, pageable)
                .map(commentMapper::toDto);
    }

    /**
     * Создает новый комментарий. Выполняет проверки существования связанной задачи и пользователя-автора.
     *
     * @param commentDto DTO комментария для создания.
     * @return Mono, содержащий DTO созданного комментария.
     */
    @Override
    @Transactional
    public Mono<CommentDto> createComment(CommentDto commentDto)  {

        Comment comment = commentMapper.toEntity(commentDto);

        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        Mono<Boolean> taskExists = taskRepository.existsById(comment.getTaskId());
        Mono<Boolean> userExists = userRepository.existsById(comment.getUserId());

        return Mono.zip(taskExists, userExists)
                .flatMap(tuple -> {
                    boolean taskFound = tuple.getT1();
                    boolean userFound = tuple.getT2();
                    if (!taskFound) {
                        return Mono.error(new ResourceNotFoundException("Task", "id", comment.getTaskId()));
                    }
                    if (!userFound) {
                        return Mono.error(new ResourceNotFoundException("User", "id", comment.getUserId()));
                    }

                    return commentRepository.save(comment)
                            .map(commentMapper::toDto);
                });

    }

    /**
     * Обновляет существующий комментарий по его идентификатору.
     *
     * @param id         Идентификатор комментария для обновления.
     * @param commentDto DTO с данными для обновления комментария.
     * @return Mono, содержащий DTO обновленного комментария, или ошибку ResourceNotFoundException, если комментарий не найден.
     */
    @Override
    @Transactional
    public Mono<CommentDto> updateComment(Long id, CommentDto commentDto) {

        return commentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("comment", "id", id)))
                .flatMap(existingComment -> {
                    commentMapper.updateEntityFromDto(commentDto, existingComment);

                    existingComment.setUpdatedAt(LocalDateTime.now());

                    return Mono.just(existingComment);
                })
                .flatMap(commentRepository::save)
                .map(commentMapper::toDto)
                ;
    }

    /**
     * Удаляет комментарий по его идентификатору.
     *
     * @param id Идентификатор комментария для удаления.
     * @return Пустой Mono, сигнализирующий о завершении операции, или ошибку ResourceNotFoundException, если комментарий не найден.
     */
    @Override
    @Transactional
    public Mono<Void> deleteComment(Long id) {

        return commentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("comment", "id", id)))
                .flatMap(existingComment -> commentRepository.deleteById(existingComment.getId()))
               ;
    }
}
