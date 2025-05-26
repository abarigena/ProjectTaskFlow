package com.abarigena.taskflow.serviceNoSQL;

import com.abarigena.taskflow.storeNoSQL.entity.DocumentFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentFileService {

    Mono<DocumentFile> uploadDocument(FilePart filePart, Long taskId);

    Flux<DocumentFile> getDocumentsByTaskId(Long taskId);

    Mono<ReactiveGridFsResource> getFileResource(ObjectId id);

    Mono<DocumentFile> getDocumentFileById(ObjectId id);

    Mono<Void> deleteDocumentFileById(ObjectId id);
}
