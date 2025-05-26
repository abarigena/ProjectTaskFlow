package com.abarigena.taskflow.mapper;

import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.storeSQL.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    UserDto toDto(User user);

    User toEntity(UserDto userDto);

    void updateEntityFromDto(UserDto userDto, @MappingTarget User user);
}
