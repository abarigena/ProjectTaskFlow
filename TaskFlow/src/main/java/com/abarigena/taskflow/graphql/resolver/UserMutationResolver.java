package com.abarigena.taskflow.graphql.resolver;

import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.graphql.type.CreateUserInput;
import com.abarigena.taskflow.graphql.type.UpdateUserInput;
import com.abarigena.taskflow.serviceSQL.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * GraphQL резолвер для Mutation операций с пользователями.
 * Все методы возвращают реактивные типы (Mono).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserMutationResolver {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Резолвер для Mutation.createUser(input: CreateUserInput!): User!
     * Создает нового пользователя.
     */
    @MutationMapping
    public Mono<UserDto> createUser(@Argument CreateUserInput input) {
        log.info("GraphQL Mutation: createUser(email={})", input.getEmail());
        
        // Создаем пользователя с зашифрованным паролем
        return userService.createUserWithPassword(
                input.getFirstName(),
                input.getLastName(),
                input.getEmail(),
                passwordEncoder.encode(input.getPassword()),  // Шифруем пароль
                input.getActive()
        )
                .doOnNext(createdUser -> log.info("Created user with id: {}", createdUser.getId()))
                .doOnError(error -> log.error("Error creating user: {}", error.getMessage()));
    }

    /**
     * Резолвер для Mutation.updateUser(id: ID!, input: UpdateUserInput!): User!
     * Обновляет существующего пользователя.
     */
    @MutationMapping
    public Mono<UserDto> updateUser(@Argument String id, @Argument UpdateUserInput input) {
        log.info("GraphQL Mutation: updateUser(id={})", id);
        
        // Создаем UserDto только с теми полями, которые нужно обновить
        UserDto updateDto = UserDto.builder()
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .email(input.getEmail())
                .active(input.getActive())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return userService.updateUser(Long.valueOf(id), updateDto)
                .doOnNext(updatedUser -> log.info("Updated user with id: {}", updatedUser.getId()))
                .doOnError(error -> log.error("Error updating user with id {}: {}", id, error.getMessage()));
    }

    /**
     * Резолвер для Mutation.deleteUser(id: ID!): Boolean!
     * Удаляет пользователя по ID.
     */
    @MutationMapping
    public Mono<Boolean> deleteUser(@Argument String id) {
        log.info("GraphQL Mutation: deleteUser(id={})", id);
        
        return userService.deleteUserById(Long.valueOf(id))
                .then(Mono.just(true))  // Возвращаем true если удаление прошло успешно
                .doOnNext(result -> log.info("Deleted user with id: {}", id))
                .doOnError(error -> log.error("Error deleting user with id {}: {}", id, error.getMessage()))
                .onErrorReturn(false);  // Возвращаем false если произошла ошибка
    }
} 