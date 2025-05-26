package com.abarigena.notificationservice.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "notification_task_history")
public class NotificationTaskHistory {

    @Id
    private Long id;

    private Long taskId;

    private String action;

    private String status;

    private Long performedBy;

    private LocalDateTime timestamp;

    private String details;
}
