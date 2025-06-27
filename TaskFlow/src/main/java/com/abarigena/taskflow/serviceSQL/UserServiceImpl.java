package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.mapper.UserMapper;
import com.abarigena.taskflow.service.ReactiveRedisService;
import com.abarigena.taskflow.storeSQL.entity.User;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ReactiveRedisService reactiveRedisService;

    private static final String USER_ID_CACHE_KEY_PREFIX = "user:id:";
    private static final String USER_EMAIL_CACHE_KEY_PREFIX = "user:email:";
    private static final Duration USER_CACHE_TTL = Duration.ofHours(24);

    /**
     * Находит всех пользователей с поддержкой пагинации и сортировки.
     *
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток всех пользователей, соответствующих параметрам пагинации, в виде DTO.
     */
    @Override
    public Flux<UserDto> findAllUsers(Pageable pageable) {
        return userRepository.findAllBy(pageable)
                .map(userMapper::toDto);
    }

    /**
     * Находит пользователя по его адресу электронной почты.
     *
     * @param email Адрес электронной почты пользователя.
     * @return Mono, содержащий DTO найденного пользователя, или ошибку ResourceNotFoundException, если пользователь не найден.
     */
    @Override
    public Mono<UserDto> findByEmail(String email) {
        String cacheKey = USER_EMAIL_CACHE_KEY_PREFIX + email;
        return reactiveRedisService.getOrSet(
                cacheKey,
                () -> userRepository.findByEmail(email)
                        .map(userMapper::toDto)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", "email", email))),
                USER_CACHE_TTL,
                UserDto.class
        );
    }

    /**
     * Создает нового пользователя. Выполняет проверку на уникальность email.
     *
     * @param userDto DTO пользователя для создания.
     * @return Mono, содержащий DTO созданного пользователя, или ошибку DataIntegrityViolationException, если email уже существует.
     */
    @Override
    @Transactional
    public Mono<UserDto> createUser(UserDto userDto) {

        User user = userMapper.toEntity(userDto);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return Mono.error(new IllegalArgumentException("Email cannot be blank"));
        }

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        if (user.getActive() == null) {
            user.setActive(true);
        }

        return userRepository.findByEmail(user.getEmail())
                .flatMap(existingUser -> Mono.<User>error(new DataIntegrityViolationException("Email" +
                        " already exists: " + user.getEmail())))
                .switchIfEmpty(userRepository.save(user))
                .cast(User.class)
                .map(userMapper::toDto);
    }

    /**
     * Находит пользователя по его идентификатору.
     *
     * @param id Идентификатор пользователя.
     * @return Mono, содержащий DTO найденного пользователя, или ошибку ResourceNotFoundException, если пользователь не найден.
     */
    @Override
    public Mono<UserDto> findUserById(Long id) {
        String cacheKey = USER_ID_CACHE_KEY_PREFIX + id;
        return reactiveRedisService.getOrSet(
                cacheKey,
                () -> userRepository.findById(id)
                        .map(userMapper::toDto)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", "id", id))),
                USER_CACHE_TTL,
                UserDto.class
        );
    }

    /**
     * Обновляет существующего пользователя по его идентификатору. Выполняет проверку на уникальность email, если он изменяется.
     *
     * @param id      Идентификатор пользователя для обновления.
     * @param userDto DTO с данными для обновления пользователя.
     * @return Mono, содержащий DTO обновленного пользователя, или ошибку ResourceNotFoundException, если пользователь не найден,
     * или DataIntegrityViolationException, если новый email уже занят.
     */
    @Override
    @Transactional
    public Mono<UserDto> updateUser(Long id, UserDto userDto) {

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", "id", id)))
                .flatMap(existingUser -> {
                    String oldEmail = existingUser.getEmail();
                    userMapper.updateEntityFromDto(userDto, existingUser);
                    existingUser.setUpdatedAt(LocalDateTime.now());

                    return userRepository.save(existingUser)
                            .flatMap(savedUser -> {
                                // При обновлении инвалидируем кэш по ID и по старому/новому email
                                return reactiveRedisService.evictAll(
                                        USER_ID_CACHE_KEY_PREFIX + savedUser.getId(),
                                        USER_EMAIL_CACHE_KEY_PREFIX + oldEmail,
                                        USER_EMAIL_CACHE_KEY_PREFIX + savedUser.getEmail()
                                ).thenReturn(savedUser);
                            });
                })
                .map(userMapper::toDto);
    }

    /**
     * Удаляет пользователя по его идентификатору.
     *
     * @param id Идентификатор пользователя для удаления.
     * @return Пустой Mono, сигнализирующий о завершении операции, или ошибку ResourceNotFoundException, если пользователь не найден.
     */
    @Override
    @Transactional
    public Mono<Void> deleteUserById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", "id", id)))
                .flatMap(existingUser ->
                        userRepository.deleteById(existingUser.getId())
                                .then(
                                        // При удалении инвалидируем кэш по ID и по email
                                        reactiveRedisService.evictAll(
                                                USER_ID_CACHE_KEY_PREFIX + existingUser.getId(),
                                                USER_EMAIL_CACHE_KEY_PREFIX + existingUser.getEmail()
                                        )
                                )
                                .then()
                );
    }
}
