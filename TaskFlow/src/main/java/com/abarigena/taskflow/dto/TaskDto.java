package com.abarigena.taskflow.dto;

import com.abarigena.taskflow.storeSQL.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskDto {
    private Long id;

    @NotBlank(message = "Название не должно быть пустым")
    private String title;

    private String description;

    private Task.Status status;

    private Task.Priority priority;

    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long assignedUserId;

    @NotNull(message = "Id проекта не должно быть пустым")
    private Long projectId;
}
