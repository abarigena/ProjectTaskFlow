package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.storeNoSQL.entity.DocumentFile;
import com.abarigena.taskflow.storeNoSQL.repository.DocumentFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentFileServiceImpl implements DocumentFileService {

    private final DocumentFileRepository documentFileRepository;
    private final ReactiveGridFsTemplate gridFsTemplate;

    /**
     * Загружает файл для задачи, сохраняет его в GridFS и метаданные в коллекцию MongoDB.
     * Выполняет базовую валидацию типа файла.
     *
     * @param filePart Представление загружаемого файла.
     * @param taskId   Идентификатор задачи (из реляционной БД), к которой привязывается документ.
     * @return Mono, содержащий сохраненные метаданные файла (DocumentFile), или ошибку в случае проблем (валидация, сохранение).
     */
    @Override
    public Mono<DocumentFile> uploadDocument(FilePart filePart, Long taskId) {
        String fileName = filePart.filename();
        String contentType = filePart.headers().getContentType() != null ?
                filePart.headers().getContentType().toString() : "application/octet-stream";

        return gridFsTemplate.store(filePart.content(), fileName, contentType)
                .flatMap(gridfsId -> {
                    return gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(gridfsId)))
                            .flatMap(gridFSFile -> {
                                if (gridFSFile == null) {
                                    log.error("Не удалось найти метаданные файла GridFS сразу после сохранения. ID: {}", gridfsId);
                                    return Mono.error(new RuntimeException("Не удалось получить метаданные сохраненного файла GridFS"));
                                }
                                long fileSize = gridFSFile.getLength();
                                log.debug("Файл '{}' сохранен в GridFS. ID: {}, Размер: {}", fileName, gridfsId, fileSize);

                                DocumentFile docMeta = DocumentFile.builder()
                                        .gridfsId(gridfsId)
                                        .taskId(taskId)
                                        .fileName(fileName)
                                        .fileType(contentType)
                                        .size(fileSize)
                                        .uploadedAt(LocalDateTime.now())
                                        .build();
                                return documentFileRepository.save(docMeta);
                            })
                            .onErrorResume(e -> {
                                log.error("Ошибка сохранения метаданных для файла GridFS ID {}, удаление файла из GridFS.", gridfsId, e);

                                return gridFsTemplate.delete(Query.query(Criteria.where("_id").is(gridfsId)))
                                        .then(Mono.error(e));
                            });
                });
    }

    /**
     * Получает список метаданных документов, связанных с указанной задачей.
     *
     * @param taskId Идентификатор задачи (из реляционной БД).
     * @return Поток метаданных документов (DocumentFile) для задачи.
     */
    @Override
    public Flux<DocumentFile> getDocumentsByTaskId(Long taskId) {
        return documentFileRepository.findByTaskId(taskId);
    }

    /**
     * Получает ресурс файла из GridFS для скачивания по идентификатору его метаданных.
     *
     * @param id Идентификатор метаданных документа (ObjectId из коллекции document_files).
     * @return Mono, содержащий ресурс файла из GridFS (ReactiveGridFsResource), или ошибку ResourceNotFoundException, если метаданные или файл GridFS не найдены.
     */
    @Override
    public Mono<ReactiveGridFsResource> getFileResource(ObjectId id) {

        return documentFileRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Метаданные документа не найдены по ID: {}", id);
                    return Mono.error(new ResourceNotFoundException("Метаданные документа", "id", id.toString()));
                }))
                .flatMap(docMeta -> {
                    ObjectId gridfsId = docMeta.getGridfsId();
                    if (gridfsId == null) {
                        log.error("Метаданные документа {} не содержат связанный ID файла GridFS.", id);
                        return Mono.error(new IllegalStateException("ID файла GridFS не найден для метаданных документа " + id));
                    }

                    log.debug("Найден gridfsId: {}. Поиск объекта GridFSFile.", gridfsId);
                    Query gridFsQuery = Query.query(Criteria.where("_id").is(gridfsId));

                    return gridFsTemplate.findOne(gridFsQuery)
                            .switchIfEmpty(Mono.defer(() -> {
                                log.error("Объект файла GridFS не найден по gridfsId: {} (ссылка из метаданных id: {}). Возможно несоответствие.", gridfsId, id);
                                return Mono.error(new ResourceNotFoundException("Файл GridFS", "gridfsId", gridfsId.toString()));
                            }))
                            .flatMap(gridFsFile -> {
                                log.debug("Найден GridFSFile: {}. Получение ресурса GridFsResource.", gridFsFile.getFilename());
                                return gridFsTemplate.getResource(gridFsFile);
                            });
                });
    }

    /**
     * Получает метаданные документа по его идентификатору.
     *
     * @param id Идентификатор метаданных документа (ObjectId из коллекции document_files).
     * @return Mono, содержащий найденные метаданные документа (DocumentFile), или ошибку ResourceNotFoundException, если метаданные не найдены.
     */
    @Override
    public Mono<DocumentFile> getDocumentFileById(ObjectId id) {
        return documentFileRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> {
                    return Mono.error(new ResourceNotFoundException("Метаданные документа", "id", id.toString()));
                }));
    }

    /**
     * Удаляет файл из GridFS и его метаданные из коллекции document_files по идентификатору метаданных.
     *
     * @param id Идентификатор метаданных документа (ObjectId из коллекции document_files).
     * @return Пустой Mono, сигнализирующий о завершении операции. Не выбрасывает ошибку, если метаданные не найдены (считается успешным удалением).
     */
    @Override
    public Mono<Void> deleteDocumentFileById(ObjectId id) {

        return documentFileRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentMetadata", "id", id.toString())))
                .flatMap(docMeta -> {
                    ObjectId gridfsId = docMeta.getGridfsId();
                    Mono<Void> deleteGridFsMono = Mono.empty();
                    if (gridfsId != null) {
                        log.debug("Найден gridfsId: {}. Удаление файла из GridFS.", gridfsId);
                        deleteGridFsMono = gridFsTemplate.delete(Query.query(Criteria.where("_id").is(gridfsId)));
                    } else {
                        log.warn("Метаданные документа {} не содержат связанный ID файла GridFS. Пропуск удаления из GridFS.", id);
                    }

                    Mono<Void> deleteMetaMono = documentFileRepository.delete(docMeta);

                    return Mono.when(deleteGridFsMono, deleteMetaMono).then();
                });
    }
}
