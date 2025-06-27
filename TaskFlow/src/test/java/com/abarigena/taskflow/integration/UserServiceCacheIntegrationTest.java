package com.abarigena.taskflow.integration;

import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.service.ReactiveRedisService;
import com.abarigena.taskflow.serviceSQL.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserServiceCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ReactiveRedisService redisService;

    @BeforeEach
    void setUp() {
        // Очищаем весь кэш перед каждым тестом
        redisService.flushAll().block();
    }

    @Test
    void testFindUserById_cachesResult() {
        // Создаем тестового пользователя
        UserDto testUser = new UserDto();
        testUser.setFirstName("Test");
        testUser.setLastName("Cache");
        testUser.setEmail("test.cache@example.com");
        
        UserDto createdUser = userService.createUser(testUser).block();
        Long userId = createdUser.getId();
        String cacheKey = "user:id:" + userId;
        
        // Первый вызов - ожидаем Cache MISS
        StepVerifier.create(userService.findUserById(userId))
                .expectNextCount(1)
                .verifyComplete();

        // Проверяем, что ключ появился в Redis
        StepVerifier.create(redisService.exists(cacheKey))
                .expectNext(true)
                .verifyComplete();

        // Второй вызов - ожидаем Cache HIT
        StepVerifier.create(userService.findUserById(userId))
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(userId);
                    assertThat(user.getFirstName()).isEqualTo("Test");
                })
                .verifyComplete();
    }

    @Test
    void testUpdateUser_invalidatesCache() {
        // Создаем тестового пользователя
        UserDto testUser = new UserDto();
        testUser.setFirstName("Test");
        testUser.setLastName("Update");
        testUser.setEmail("test.update@example.com");
        
        UserDto createdUser = userService.createUser(testUser).block();
        Long userId = createdUser.getId();
        String cacheKey = "user:id:" + userId;
        
        // 1. Заполняем кэш
        userService.findUserById(userId).block();
        StepVerifier.create(redisService.exists(cacheKey))
                .expectNext(true)
                .verifyComplete();

        // 2. Обновляем пользователя
        UserDto updatedDto = new UserDto();
        updatedDto.setFirstName("Updated Name");
        userService.updateUser(userId, updatedDto).block();

        // 3. Проверяем, что кэш был инвалидирован
        StepVerifier.create(
                Mono.delay(Duration.ofMillis(500))
                        .then(redisService.exists(cacheKey))
            )
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void testDeleteUser_invalidatesCache() {
        // Создаем нового пользователя для удаления
        UserDto newUser = new UserDto();
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        newUser.setEmail("test.delete@example.com");
        
        UserDto createdUser = userService.createUser(newUser).block();
        Long userId = createdUser.getId();
        String cacheKey = "user:id:" + userId;

        // 1. Заполняем кэш
        userService.findUserById(userId).block();
        StepVerifier.create(redisService.exists(cacheKey))
                .expectNext(true)
                .verifyComplete();

        // 2. Удаляем пользователя
        userService.deleteUserById(userId).block();

        // 3. Проверяем, что кэш был инвалидирован
        StepVerifier.create(
                Mono.delay(Duration.ofMillis(500))
                        .then(redisService.exists(cacheKey))
            )
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void testFindUserById_setsCorrectTtl() {
        // Создаем тестового пользователя
        UserDto testUser = new UserDto();
        testUser.setFirstName("Test");
        testUser.setLastName("TTL");
        testUser.setEmail("test.ttl@example.com");
        
        UserDto createdUser = userService.createUser(testUser).block();
        Long userId = createdUser.getId();
        String cacheKey = "user:id:" + userId;
        
        // Вызываем метод, чтобы заполнить кэш
        userService.findUserById(userId).block();

        // Проверяем TTL (допускаем небольшие отклонения из-за задержек)
        StepVerifier.create(redisService.getTtl(cacheKey))
                .assertNext(ttl -> {
                    // TTL должен быть между 23 и 24 часами (учитываем задержки)
                    long hours = ttl.toHours();
                    assertThat(hours).isBetween(23L, 24L);
                })
                .verifyComplete();
    }
} 