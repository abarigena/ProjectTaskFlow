package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.DocumentFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentFileService {

    /**
     * Загружает документ для указанной задачи.
     * @param filePart файл для загрузки
     * @param taskId идентификатор задачи
     * @return моно с метаданными загруженного файла
     */
    Mono<DocumentFile> uploadDocument(FilePart filePart, Long taskId);

    /**
     * Получает список документов для указанной задачи.
     * @param taskId идентификатор задачи
     * @return поток метаданных документов
     */
    Flux<DocumentFile> getDocumentsByTaskId(Long taskId);

    /**
     * Получает ресурс файла из GridFS по его идентификатору.
     * @param id идентификатор файла в GridFS (не путать с ID метаданных)
     * @return моно с ресурсом файла
     */
    Mono<ReactiveGridFsResource> getFileResource(ObjectId id);

    /**
     * Получает метаданные документа по их идентификатору.
     * @param id идентификатор метаданных документа
     * @return моно с метаданными документа
     */
    Mono<DocumentFile> getDocumentFileById(ObjectId id);

    /**
     * Удаляет документ и его метаданные по идентификатору метаданных.
     * @param id идентификатор метаданных документа
     * @return моно без содержимого
     */
    Mono<Void> deleteDocumentFileById(ObjectId id);
}
