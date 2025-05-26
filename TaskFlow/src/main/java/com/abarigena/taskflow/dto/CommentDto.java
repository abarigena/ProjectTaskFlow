package com.abarigena.taskflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {

    private Long id;

    private String context;

    private Long taskId;

    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
