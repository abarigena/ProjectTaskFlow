package com.abarigena.taskflow.dto;

import com.abarigena.taskflow.storeSQL.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {

    private Long id;

    private String name;

    private String description;

    private Project.Status status;

    private Long ownerId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
