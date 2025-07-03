package com.abarigena.taskflow.graphql.resolver;

import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.serviceSQL.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GraphQL резолвер для Query операций с пользователями.
 * Все методы возвращают реактивные типы (Mono/Flux).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserQueryResolver {

    private final UserService userService;

    /**
     * Резолвер для Query.user(id: ID!): User
     * Получает пользователя по ID.
     */
    @QueryMapping
    public Mono<UserDto> user(@Argument String id) {
        log.info("GraphQL Query: user(id={})", id);
        
        return userService.findUserById(Long.valueOf(id))
                .doOnNext(user -> log.debug("Found user: {}", user.getEmail()))
                .doOnError(error -> log.error("Error finding user with id {}: {}", id, error.getMessage()));
    }

    /**
     * Резолвер для Query.users(limit: Int, offset: Int): [User!]!
     * Получает список пользователей с пагинацией.
     */
    @QueryMapping
    public Flux<UserDto> users(@Argument Integer limit, @Argument Integer offset) {
        log.info("GraphQL Query: users(limit={}, offset={})", limit, offset);
        
        // Создаем Pageable из переданных параметров
        Pageable pageable = PageRequest.of(
                offset / limit,  // номер страницы
                limit           // размер страницы
        );
        
        return userService.findAllUsers(pageable)
                .doOnNext(user -> log.debug("Retrieved user: {}", user.getEmail()))
                .doOnComplete(() -> log.debug("Completed users query"))
                .doOnError(error -> log.error("Error retrieving users: {}", error.getMessage()));
    }

    /**
     * Резолвер для Query.userByEmail(email: String!): User
     * Находит пользователя по email.
     */
    @QueryMapping
    public Mono<UserDto> userByEmail(@Argument String email) {
        log.info("GraphQL Query: userByEmail(email={})", email);
        
        return userService.findByEmail(email)
                .doOnNext(user -> log.debug("Found user by email: {}", user.getEmail()))
                .doOnError(error -> log.error("Error finding user by email {}: {}", email, error.getMessage()));
    }
} 