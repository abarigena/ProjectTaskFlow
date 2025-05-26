package com.abarigena.taskflow.mapper;

import com.abarigena.taskflow.dto.TaskDto;
import com.abarigena.taskflow.storeSQL.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskMapper {

    TaskDto toDto(Task task);

    Task toEntity(TaskDto taskDto);

    void updateEntityFromDto(TaskDto taskDto, @MappingTarget Task task);
}
