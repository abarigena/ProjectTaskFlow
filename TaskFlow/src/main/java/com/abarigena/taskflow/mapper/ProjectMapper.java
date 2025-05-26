package com.abarigena.taskflow.mapper;

import com.abarigena.taskflow.dto.ProjectDto;
import com.abarigena.taskflow.storeSQL.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProjectMapper {
    ProjectDto toDto(Project project);

    Project toEntity(ProjectDto projectDto);

    void updateEntityFromDto(ProjectDto projectDto, @MappingTarget Project project);
}
