package com.abarigena.taskflow.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input тип для создания нового пользователя через GraphQL.
 * Соответствует типу CreateUserInput в schema.graphqls
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserInput {
    
    private String firstName;
    
    private String lastName;
    
    private String email;
    
    private String password;
    
    @Builder.Default
    private Boolean active = true;
} 