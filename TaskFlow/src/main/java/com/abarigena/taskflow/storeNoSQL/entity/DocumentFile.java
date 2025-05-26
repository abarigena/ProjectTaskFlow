package com.abarigena.taskflow.storeNoSQL.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "document_files")
public class    DocumentFile {
    @Id
    private ObjectId id;

    private ObjectId gridfsId;

    private Long taskId;

    private String fileName;

    private String fileType;

    private LocalDateTime uploadedAt;

    private Long size;
}
