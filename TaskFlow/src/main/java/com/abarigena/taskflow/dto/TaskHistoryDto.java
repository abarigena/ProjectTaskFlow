package com.abarigena.taskflow.dto;

import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskHistoryDto {
    private Long taskId;

    private TaskHistory.Action action;

    private Long performedBy;

    private String status;

    private LocalDateTime timestamp;

    private Map<String, Object> details;
}
