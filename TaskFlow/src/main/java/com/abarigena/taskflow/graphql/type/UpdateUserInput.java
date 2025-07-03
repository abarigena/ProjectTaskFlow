package com.abarigena.taskflow.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input тип для обновления существующего пользователя через GraphQL.
 * Соответствует типу UpdateUserInput в schema.graphqls
 * Все поля опциональные - обновляются только переданные значения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserInput {
    
    private String firstName;
    
    private String lastName;
    
    private String email;
    
    private Boolean active;
} 