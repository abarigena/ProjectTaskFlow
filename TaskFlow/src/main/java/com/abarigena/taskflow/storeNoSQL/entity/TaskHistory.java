package com.abarigena.taskflow.storeNoSQL.entity;

import com.abarigena.taskflow.storeSQL.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "task_histories")
public class TaskHistory {

    @Id
    private ObjectId id;

    private Long taskId;

    private Action action; //Create, Update, Delete

    private Long performedBy;

    private LocalDateTime timestamp;

    private String status;

    private Map<String, Object> details;

    public enum Action {
        CREATE,
        UPDATE,
        DELETE
    }
}
