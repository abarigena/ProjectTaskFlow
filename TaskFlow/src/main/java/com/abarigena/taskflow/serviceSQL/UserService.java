package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.UserDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    /**
     * Находит всех пользователей с использованием пагинации.
     * @param pageable параметры пагинации
     * @return поток DTO пользователей
     */
    Flux<UserDto> findAllUsers(Pageable pageable);

    /**
     * Находит пользователя по его адресу электронной почты.
     * @param email адрес электронной почты
     * @return моно DTO пользователя
     */
    Mono<UserDto> findByEmail(String email);

    /**
     * Создает нового пользователя.
     * @param userDto DTO пользователя
     * @return моно DTO созданного пользователя
     */
    Mono<UserDto> createUser(UserDto userDto);

    /**
     * Находит пользователя по его идентификатору.
     * @param id идентификатор пользователя
     * @return моно DTO пользователя
     */
    Mono<UserDto> findUserById(Long id);

    /**
     * Обновляет существующего пользователя.
     * @param id идентификатор пользователя
     * @param userDto DTO пользователя с обновленными данными
     * @return моно DTO обновленного пользователя
     */
    Mono<UserDto> updateUser(Long id, UserDto userDto);

    /**
     * Удаляет пользователя по его идентификатору.
     * @param id идентификатор пользователя
     * @return моно без содержимого
     */
    Mono<Void> deleteUserById(Long id);
}
