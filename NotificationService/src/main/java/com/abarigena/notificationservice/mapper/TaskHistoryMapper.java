package com.abarigena.notificationservice.mapper;

import com.abarigena.notificationservice.dto.TaskHistoryDto;
import com.abarigena.notificationservice.store.entity.NotificationTaskHistory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Mapper(componentModel = "spring")
public abstract class TaskHistoryMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "action", target = "action", qualifiedByName = "actionToString")
    @Mapping(source = "details", target = "details", qualifiedByName = "mapToJsonString")
    public abstract NotificationTaskHistory toEntity(TaskHistoryDto dto);

    @Named("actionToString")
    protected String actionToString(Object action) {
        return (action != null) ? action.toString() : null;
    }

    @Named("mapToJsonString")
    protected String mapToJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации деталей задачи в JSON", e);
        }
    }
} 