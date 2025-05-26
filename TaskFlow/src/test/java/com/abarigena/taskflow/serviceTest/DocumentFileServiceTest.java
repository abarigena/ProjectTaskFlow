package com.abarigena.taskflow.serviceTest;

import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.serviceNoSQL.DocumentFileServiceImpl;
import com.abarigena.taskflow.storeNoSQL.entity.DocumentFile;
import com.abarigena.taskflow.storeNoSQL.repository.DocumentFileRepository;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Модульные тесты для GridFS")
class DocumentFileServiceTest {

    @Mock
    private DocumentFileRepository documentFileRepository;

    @Mock
    private ReactiveGridFsTemplate gridFsTemplate;

    @InjectMocks
    private DocumentFileServiceImpl documentFileService;

    private Long taskId;
    private ObjectId documentId;
    private ObjectId gridfsId;
    private String filename;
    private String contentType;
    private FilePart mockFilePart;
    private Flux<DataBuffer> mockDataBufferFlux;
    private GridFSFile mockGridFsFile;
    private DocumentFile documentFileEntity;
    private ReactiveGridFsResource mockResource;

    @BeforeEach
    void setUp() {
        taskId = 1L;
        documentId = new ObjectId();
        gridfsId = new ObjectId();
        filename = "test-file.png";
        contentType = "image/png";

        // --FilePart
        mockFilePart = mock(FilePart.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));
        lenient().when(mockFilePart.filename()).thenReturn(filename);
        lenient().when(mockFilePart.headers()).thenReturn(httpHeaders);

        mockDataBufferFlux = Flux.just(new DefaultDataBufferFactory().wrap("test content".getBytes(StandardCharsets.UTF_8)));
        lenient().when(mockFilePart.content()).thenReturn(mockDataBufferFlux);

        documentFileEntity = DocumentFile.builder()
                .gridfsId(gridfsId)
                .taskId(taskId)
                .fileName(filename)
                .fileType(contentType)
                .size(14L)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("uploadDocument - Должен успешно сохранить файл и метаданные")
    void uploadDocument_ValidInput_ShouldStoreFileAndMetadata() {

        // --GridFs
        mockGridFsFile = mock(GridFSFile.class);
        when(mockGridFsFile.getLength()).thenReturn(14L);

        when(gridFsTemplate.store(eq(mockDataBufferFlux), eq(filename), eq(contentType)))
                .thenReturn(Mono.just(gridfsId));

        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.just(mockGridFsFile));

        DocumentFile savedDocumnetFile = DocumentFile.builder()
                .id(new ObjectId())
                .gridfsId(documentFileEntity.getGridfsId())
                .taskId(documentFileEntity.getTaskId())
                .fileName(documentFileEntity.getFileName())
                .fileType(documentFileEntity.getFileType())
                .size(documentFileEntity.getSize())
                .uploadedAt(documentFileEntity.getUploadedAt())
                .build();

        when(documentFileRepository.save(any(DocumentFile.class))).thenReturn(Mono.just(savedDocumnetFile));

        Mono<DocumentFile> resultMono = documentFileService.uploadDocument(mockFilePart, taskId);

        StepVerifier.create(resultMono)
                .expectNextMatches(savedMeta -> {
                    assertThat(savedMeta.getId()).isEqualTo(savedDocumnetFile.getId());
                    assertThat(savedMeta.getTaskId()).isEqualTo(taskId);
                    assertThat(savedMeta.getFileName()).isEqualTo(filename);
                    assertThat(savedMeta.getFileType()).isEqualTo(contentType);
                    assertThat(savedMeta.getSize()).isEqualTo(14L);
                    assertThat(savedMeta.getGridfsId()).isEqualTo(gridfsId);
                    assertThat(savedMeta.getUploadedAt()).isNotNull();
                    return true;
                })
                .verifyComplete();

        verify(gridFsTemplate).store(eq(mockDataBufferFlux), eq(filename), eq(contentType));

        verify(gridFsTemplate).findOne(any(Query.class));

        ArgumentCaptor<DocumentFile> docCaptor = ArgumentCaptor.forClass(DocumentFile.class);
        verify(documentFileRepository).save(docCaptor.capture());
        DocumentFile captureDoc = docCaptor.getValue();
        assertThat(captureDoc.getGridfsId()).isEqualTo(gridfsId);
        assertThat(captureDoc.getTaskId()).isEqualTo(taskId);
        assertThat(captureDoc.getFileName()).isEqualTo(filename);

        verify(gridFsTemplate, never()).delete(any(Query.class));
    }

    @Test
    @DisplayName("uploadDocument - должен удалить файл из GridFS и вернуть ошибку, если сохранение метаданных не удалось")
    void uploadDocument_MetadataSaveError_ShouldDeleteGridFsFileAndReturnError() {

        RuntimeException dbError = new RuntimeException("exception при сохранении данных");
        Long expectedSize = 20L;

        mockGridFsFile = mock(GridFSFile.class);
        when(mockGridFsFile.getLength()).thenReturn(expectedSize);
        when(gridFsTemplate.store(eq(mockDataBufferFlux), eq(filename), eq(contentType)))
                .thenReturn(Mono.just(gridfsId));

        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.just(mockGridFsFile));
        when(gridFsTemplate.delete(any(Query.class)))
                .thenReturn(Mono.empty());

        when(documentFileRepository.save(any(DocumentFile.class))).thenThrow(dbError);

        Mono<DocumentFile> resultMono = documentFileService.uploadDocument(mockFilePart, taskId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().equals(dbError.getMessage()))
                .verify();

        verify(gridFsTemplate).store(eq(mockDataBufferFlux), eq(filename), eq(contentType));
        verify(gridFsTemplate).findOne(any(Query.class));
        verify(documentFileRepository).save(any(DocumentFile.class));

        ArgumentCaptor<Query> deleteQueryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(gridFsTemplate).delete(deleteQueryCaptor.capture());

        Query captureDeleteQuery = deleteQueryCaptor.getValue();
        assertThat(captureDeleteQuery.getQueryObject().get("_id")).isEqualTo(gridfsId);

        verify(gridFsTemplate, never()).getResource(any(GridFSFile.class));

        verify(documentFileRepository, never()).findById(any(ObjectId.class));
        verify(documentFileRepository, never()).delete(any(DocumentFile.class));
    }

    @Test
    @DisplayName("deleteDocumentFileById - должен успешно удалить метаданные и файл из GridFS")
    void deleteDocumentFileById_ExistsWithGridFsId_ShouldDeleteBoth() {

        when(documentFileRepository.findById(documentId)).thenReturn(Mono.just(documentFileEntity));

        when(gridFsTemplate.delete(any(Query.class))).thenReturn(Mono.empty());

        when(documentFileRepository.delete(documentFileEntity)).thenReturn(Mono.empty());

        Mono<Void> resultMono = documentFileService.deleteDocumentFileById(documentId);

        StepVerifier.create(resultMono)
                .verifyComplete();

        verify(documentFileRepository).findById(documentId);

        ArgumentCaptor<Query> deleteQueryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(gridFsTemplate).delete(deleteQueryCaptor.capture());
        Query captureDeleteQuery = deleteQueryCaptor.getValue();

        assertThat(captureDeleteQuery.getQueryObject().get("_id")).isEqualTo(documentFileEntity.getGridfsId());

        verify(documentFileRepository).delete(documentFileEntity);
    }

    @Test
    @DisplayName("deleteDocumentFileById - должен успешно удалить только метаданные, если gridfsId null")
    void deleteDocumentFileById_ExistsWithoutGridFsId_ShouldDeleteMetaOnly() {
        DocumentFile metaWithoutGridFs = DocumentFile.builder()
                .id(documentId)
                .taskId(taskId)
                .fileName("meta-only.png")
                .build();

        when(documentFileRepository.findById(documentId)).thenReturn(Mono.just(metaWithoutGridFs));

        when(documentFileRepository.delete(metaWithoutGridFs)).thenReturn(Mono.empty());

        Mono<Void> resultMono = documentFileService.deleteDocumentFileById(documentId);

        StepVerifier.create(resultMono)
                .verifyComplete();

        verify(documentFileRepository).findById(documentId);
        verify(gridFsTemplate, never()).delete(any(Query.class));

        verify(documentFileRepository).delete(metaWithoutGridFs);
    }

    @Test
    @DisplayName("deleteDocumentFileById - должен вернуть ошибку ResourceNotFoundException, если метаданные не найдены")
    void deleteDocumentFileById_MetadataNotFound_ShouldReturnError() {
        ObjectId nonExistingId = new ObjectId();

        when(documentFileRepository.findById(nonExistingId)).thenReturn(Mono.empty());

        Mono<Void> resultMono = documentFileService.deleteDocumentFileById(nonExistingId);

        StepVerifier.create(resultMono)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(documentFileRepository).findById(nonExistingId);
        verifyNoInteractions(gridFsTemplate);

        verify(documentFileRepository, never()).delete(any(DocumentFile.class));
    }

    @Test
    @DisplayName("deleteDocumentFileById - должен пробросить ошибку при ошибке удаления из GridFS")
    void deleteDocumentFileById_GridFsDeleteError_ShouldPropagateError() {
        RuntimeException gridFsError = new RuntimeException("Ошибка удаления GridFS");

        when(documentFileRepository.findById(documentId)).thenReturn(Mono.just(documentFileEntity));

        when(gridFsTemplate.delete(any(Query.class))).thenReturn(Mono.error(gridFsError));

        when(documentFileRepository.delete(documentFileEntity)).thenReturn(Mono.empty());

        Mono<Void> resultMono = documentFileService.deleteDocumentFileById(documentId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().equals(gridFsError.getMessage()))
                .verify();

        verify(documentFileRepository).findById(documentId);
        verify(gridFsTemplate).delete(any(Query.class));

        verify(documentFileRepository).delete(documentFileEntity);
    }

    @Test
    @DisplayName("deleteDocumentFileById - должен пробросить ошибку при ошибке удаления метаданных")
    void deleteDocumentFileById_MetadataDeleteError_ShouldPropagateError() {
        RuntimeException repoError = new RuntimeException("Ошибка удаления метаданных");

        when(documentFileRepository.findById(documentId)).thenReturn(Mono.just(documentFileEntity));

        when(gridFsTemplate.delete(any(Query.class))).thenReturn(Mono.empty());

        when(documentFileRepository.delete(documentFileEntity)).thenReturn(Mono.error(repoError));

        Mono<Void> resultMono = documentFileService.deleteDocumentFileById(documentId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().equals(repoError.getMessage()))
                .verify();

        verify(documentFileRepository).findById(documentId);
        verify(gridFsTemplate).delete(any(Query.class));
        verify(documentFileRepository).delete(documentFileEntity);
    }

    @Test
    @DisplayName("getFileResource - должен успешно вернуть ресурс файла по ID метаданных")
    void getFileResource_Exists_ShouldReturnResource() {
        GridFSFile mockGridFsFile = mock(GridFSFile.class);
        ReactiveGridFsResource mockResource = mock(ReactiveGridFsResource.class);

        when(documentFileRepository.findById(documentId)).thenReturn(Mono.just(documentFileEntity));

        Query exceptedQuery = Query.query(Criteria.where("_id").is(gridfsId));
        when(gridFsTemplate.findOne(eq(exceptedQuery))).thenReturn(Mono.just(mockGridFsFile));

        when(gridFsTemplate.getResource(mockGridFsFile)).thenReturn(Mono.just(mockResource));

        Mono<ReactiveGridFsResource> resultMono = documentFileService.getFileResource(documentId);

        StepVerifier.create(resultMono)
                .expectNext(mockResource)
                .verifyComplete();

        verify(documentFileRepository).findById(documentId);
        verify(gridFsTemplate).findOne(eq(exceptedQuery));
        verify(gridFsTemplate).getResource(eq(mockGridFsFile));
    }

    @Test
    @DisplayName("getFileResource - должен вернуть ошибку ResourceNotFoundException, если метаданные не найдены")
    void getFileResource_MetadataNotFound_ShouldReturnError() {
        ObjectId nonExistingId = new ObjectId();

        when(documentFileRepository.findById(nonExistingId)).thenReturn(Mono.empty());

        Mono<ReactiveGridFsResource> resultMono = documentFileService.getFileResource(nonExistingId);

        StepVerifier.create(resultMono)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(documentFileRepository).findById(nonExistingId);
        verifyNoInteractions(gridFsTemplate);
    }

    @Test
    @DisplayName("getFileResource - должен вернуть ошибку IllegalStateException, если gridfsId в метаданных null")
    void getFileResource_MetadataHasNullGridFsId_ShouldReturnError() {
        DocumentFile metaWithoutGridFs = DocumentFile.builder()
                .id(documentId)
                .taskId(taskId)
                .fileName("meta-only.txt")
                .gridfsId(null)
                .build();

        when(documentFileRepository.findById(documentId)).thenReturn(Mono.just(metaWithoutGridFs));

        Mono<ReactiveGridFsResource> resultMono = documentFileService.getFileResource(documentId);

        StepVerifier.create(resultMono)
                .expectError(IllegalStateException.class)
                .verify();

        verify(documentFileRepository).findById(documentId);
        verifyNoInteractions(gridFsTemplate);
    }

    @Test
    @DisplayName("getFileResource - должен вернуть ошибку ResourceNotFoundException, если файл GridFS не найден")
    void getFileResource_GridFsFileNotFound_ShouldReturnError() {
        GridFSFile mockGridFsFile = mock(GridFSFile.class);
        ReactiveGridFsResource mockResource = mock(ReactiveGridFsResource.class);

        when(documentFileRepository.findById(documentId)).thenReturn(Mono.just(documentFileEntity));

        Query expectedQuery = Query.query(Criteria.where("_id").is(gridfsId));
        when(gridFsTemplate.findOne(expectedQuery)).thenReturn(Mono.empty());

        Mono<ReactiveGridFsResource> resultMono = documentFileService.getFileResource(documentId);

        StepVerifier.create(resultMono)
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(documentFileRepository).findById(documentId);
        verify(gridFsTemplate).findOne(expectedQuery);
        verify(gridFsTemplate, never()).getResource(any(GridFSFile.class));
    }

    @Test
    @DisplayName("getFileResource - должен пробросить ошибку, если getResource вернул ошибку")
    void getFileResource_GridFsGetResourceError_ShouldPropagateError() {
        GridFSFile mockGridFsFile = mock(GridFSFile.class);
        ReactiveGridFsResource mockResource = mock(ReactiveGridFsResource.class);

        RuntimeException gridFsError = new RuntimeException("Ошибка получения ресурса GridFS");

        when(documentFileRepository.findById(documentId)).thenReturn(Mono.just(documentFileEntity));

        Query expectedQuery = Query.query(Criteria.where("_id").is(gridfsId));
        when(gridFsTemplate.findOne(eq(expectedQuery))).thenReturn(Mono.just(mockGridFsFile));

        when(gridFsTemplate.getResource(mockGridFsFile)).thenReturn(Mono.error(gridFsError));

        Mono<ReactiveGridFsResource> resultMono = documentFileService.getFileResource(documentId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().equals(gridFsError.getMessage()))
                .verify();

        verify(documentFileRepository).findById(documentId);
        verify(gridFsTemplate).findOne(eq(expectedQuery));
        verify(gridFsTemplate).getResource(mockGridFsFile);
    }
}
