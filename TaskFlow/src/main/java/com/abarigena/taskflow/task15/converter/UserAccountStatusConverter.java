package com.abarigena.taskflow.task15.converter;

import com.abarigena.taskflow.task15.types.UserAccountStatus;
import org.jooq.impl.EnumConverter;

public class UserAccountStatusConverter extends EnumConverter<String, UserAccountStatus> {
    public UserAccountStatusConverter() {
        super(String.class, UserAccountStatus.class);
    }
} 