package com.abarigena.taskflow.mapper;

import com.abarigena.taskflow.dto.CommentDto;
import com.abarigena.taskflow.storeSQL.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CommentMapper {

    CommentDto toDto(Comment comment);

    Comment toEntity(CommentDto commentDto);

    void updateEntityFromDto(CommentDto commentDto, @MappingTarget Comment comment);
}
