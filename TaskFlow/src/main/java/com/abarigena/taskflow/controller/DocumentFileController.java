package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.serviceNoSQL.DocumentFileService;
import com.abarigena.taskflow.storeNoSQL.entity.DocumentFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import com.mongodb.client.gridfs.model.GridFSFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DocumentFileController {
    private final DocumentFileService documentFileService;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final List<MediaType> ALLOWED_MEDIA_TYPES = List.of(
            MediaType.APPLICATION_PDF,
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG
    );

    private boolean isMediaTypeAllowed(MediaType mediaType) {
        return ALLOWED_MEDIA_TYPES.stream().anyMatch(allowed -> allowed.includes(mediaType));
    }

    /**
     * Загружает документ для указанной задачи.
     * @param taskId идентификатор задачи
     * @param filePartMono моно с файлом для загрузки
     * @return моно с метаданными загруженного файла
     */
    @PostMapping(value = "/tasks/{taskId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DocumentFile> uploadDocument(
            @PathVariable Long taskId,
            @RequestPart("file") Mono<FilePart> filePartMono
    ){
        log.info("Запрос на загрузку документа для задачи ID: {}", taskId);

        return filePartMono
                .flatMap(filePart -> {
                    MediaType contentType = filePart.headers().getContentType();
                    if (contentType == null || !isMediaTypeAllowed(contentType)) {
                        log.warn("Загрузка отклонена для задачи {}: Недопустимый тип файла {}", taskId, contentType);
                        return Mono.error(new ServerWebInputException("Недопустимый тип файла. Разрешены: " + ALLOWED_MEDIA_TYPES));
                    }

                    log.debug("Загрузка файла '{}' с типом {}", filePart.filename(), contentType);
                    return documentFileService.uploadDocument(filePart, taskId);
                })
                .onErrorMap(DataBufferLimitException.class, ex -> {
                    log.warn("Загрузка отклонена для задачи {}: Размер файла превышает лимит {} МБ", taskId, (MAX_FILE_SIZE_BYTES / 1024 / 1024));
                    return new ServerWebInputException("Размер файла превышает лимит " + (MAX_FILE_SIZE_BYTES / 1024 / 1024) + " МБ");
                })
                .doOnSuccess(meta -> log.info("Метаданные для загруженного файла задачи {} сохранены: ID {}", taskId, meta.getId()))
                .doOnError(error -> !(error instanceof ServerWebInputException || error instanceof DataBufferLimitException),
                        error -> log.error("Непредвиденная ошибка при загрузке файла для задачи {}: {}", taskId, error.getMessage(), error));
    }

    /**
     * Получает список документов для указанной задачи.
     * @param taskId идентификатор задачи
     * @return поток метаданных документов
     */
    @GetMapping("/tasks/{taskId}/documents")
    public Flux<DocumentFile> getDocumentsForTask(@PathVariable Long taskId) {
        log.info("Запрос списка документов для задачи ID: {}", taskId);
        // TODO: Добавить проверку существования задачи taskId
        return documentFileService.getDocumentsByTaskId(taskId);
    }

    /**
     * Скачивает документ по его идентификатору.
     * @param id идентификатор документа
     * @return моно с ResponseEntity, содержащим поток данных файла
     */
    @GetMapping("/documents/{id}/download")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadDocument(@PathVariable String id) {
        log.info("Запрос на скачивание документа по ID метаданных: {}", id);
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.warn("Неверный формат ObjectId при запросе на скачивание: {}", id);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return documentFileService.getFileResource(objectId)
                .flatMap(resource -> {

                    Mono<GridFSFile> gridFSFileMono = resource.getGridFSFile();

                    return gridFSFileMono.flatMap(gridFSFile -> {
                        if (gridFSFile == null) {
                            log.error("GridFSFile из Mono оказался null для ресурса документа ID: {}", id);
                            return Mono.error(new IllegalStateException("Не удалось получить GridFSFile из ресурса"));
                        }

                        String filename = gridFSFile.getFilename();
                        long length = gridFSFile.getLength();

                        Document metadata = gridFSFile.getMetadata();
                        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                        if (metadata != null && metadata.containsKey("_contentType")) {
                            Object contentTypeObj = metadata.get("_contentType");
                            if (contentTypeObj instanceof String) {
                                contentType = (String) contentTypeObj;
                            } else {
                                log.warn("Поле _contentType в метаданных GridFSFile не является строкой для файла {}, ID: {}", filename, gridFSFile.getId());
                            }
                        } else {
                            log.warn("Метаданные GridFSFile или поле _contentType отсутствуют для файла {}, ID: {}", filename, gridFSFile.getId());
                        }

                        log.debug("Найден ресурс для скачивания: Имя='{}', Тип='{}', Размер={}", filename, contentType, length);

                        Flux<DataBuffer> downloadStream = resource.getDownloadStream();

                        return Mono.just(ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_TYPE, contentType)
                                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(length))
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                                .body(downloadStream));
                    });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Сервис getFileResource вернул пустой Mono для ID: {}", id);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                }));
    }

    /**
     * Получает метаданные документа по его идентификатору.
     * @param id идентификатор документа
     * @return моно с ResponseEntity, содержащим метаданные документа
     */
    @GetMapping("/documents/{id}")
    public Mono<ResponseEntity<DocumentFile>> getDocumentMetadata(@PathVariable String id) {
        log.info("Запрос метаданных документа по ID: {}", id);
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.warn("Неверный формат ObjectId при запросе метаданных: {}", id);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return documentFileService.getDocumentFileById(objectId)
                .map(ResponseEntity::ok);
    }

    /**
     * Удаляет документ по его идентификатору.
     * @param id идентификатор документа
     * @return моно без содержимого
     */
    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteDocument(@PathVariable String id) {
        log.warn("Запрос на удаление документа по ID метаданных: {}", id);
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            log.warn("Неверный формат ObjectId при запросе на удаление: {}", id);
            return Mono.error(new ServerWebInputException("Неверный формат ID документа"));
        }
        return documentFileService.deleteDocumentFileById(objectId);
    }
}
