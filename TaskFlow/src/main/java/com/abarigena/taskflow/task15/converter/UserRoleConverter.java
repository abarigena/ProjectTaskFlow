package com.abarigena.taskflow.task15.converter;

import com.abarigena.taskflow.task15.types.UserRole;
import org.jooq.impl.EnumConverter;

public class UserRoleConverter extends EnumConverter<String, UserRole> {
    public UserRoleConverter() {
        super(String.class, UserRole.class);
    }
} 