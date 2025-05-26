package com.abarigena.notificationservice.store.repository;

import com.abarigena.notificationservice.store.entity.NotificationTaskHistory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationTaskHistoryRepository extends R2dbcRepository<NotificationTaskHistory, Long> {

}
