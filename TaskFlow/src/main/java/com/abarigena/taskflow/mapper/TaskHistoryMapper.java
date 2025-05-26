package com.abarigena.taskflow.mapper;

import com.abarigena.taskflow.dto.TaskHistoryDto;
import com.abarigena.taskflow.storeNoSQL.entity.TaskHistory;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskHistoryMapper {

    TaskHistoryDto toDto(TaskHistory taskHistory);

    TaskHistory toEntity(TaskHistoryDto taskHistoryDto);
}
