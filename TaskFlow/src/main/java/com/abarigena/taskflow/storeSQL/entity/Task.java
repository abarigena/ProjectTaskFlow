package com.abarigena.taskflow.storeSQL.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name ="tasks")
public class Task {

    @Id
    private Long id;

    private String title;

    private String description;

    private Status status;

    private Priority priority;

    private LocalDateTime deadline;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("assigned_user_id")
    private Long assignedUserId;

    @Column("project_id")
    private Long projectId;

    public enum Status {
        TODO,
        IN_PROGRESS,
        DONE
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

}
