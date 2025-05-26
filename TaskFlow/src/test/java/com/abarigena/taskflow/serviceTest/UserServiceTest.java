package com.abarigena.taskflow.serviceTest;

import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.exception.ResourceNotFoundException;
import com.abarigena.taskflow.mapper.UserMapper;
import com.abarigena.taskflow.serviceSQL.UserServiceImpl;
import com.abarigena.taskflow.storeSQL.entity.User;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit тесты для UserServiceImpl")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User userEntity;
    private UserDto userDto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userEntity = new User();
        userEntity.setId(1L);
        userEntity.setEmail("test@example.com");
        userEntity.setFirstName("Test");
        userEntity.setLastName("User");
        userEntity.setActive(true);
        userEntity.setCreatedAt(LocalDateTime.now());
        userEntity.setUpdatedAt(LocalDateTime.now());

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setEmail("test@example.com");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setActive(true);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    @DisplayName("findAllUsers должен возвращать Flux с UserDto")
    void findAllUsers_shouldReturnFluxOfUserDtos() {
        User anotherUserEntity = new User();
        anotherUserEntity.setId(2L);
        anotherUserEntity.setEmail("another@example.com");
        UserDto anotherUserDto = new UserDto();
        anotherUserDto.setId(2L);
        anotherUserDto.setEmail("another@example.com");

        when(userRepository.findAllBy(any(Pageable.class)))
                .thenReturn(Flux.just(userEntity, anotherUserEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);
        when(userMapper.toDto(anotherUserEntity)).thenReturn(anotherUserDto);

        Flux<UserDto> result = userService.findAllUsers(pageable);

        StepVerifier.create(result)
                .expectNext(userDto)
                .expectNext(anotherUserDto)
                .verifyComplete();

        verify(userRepository).findAllBy(pageable);
        verify(userMapper, times(2)).toDto(any(User.class));
    }

    @Test
    @DisplayName("findByEmail должен возвращать UserDto, если пользователь найден")
    void findByEmail_shouldReturnUserDto_whenUserFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Mono.just(userEntity));
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        Mono<UserDto> result = userService.findByEmail("test@example.com");

        StepVerifier.create(result)
                .expectNext(userDto)
                .verifyComplete();

        verify(userRepository).findByEmail("test@example.com");
        verify(userMapper).toDto(userEntity);
    }

    @Test
    @DisplayName("findByEmail должен возвращать ResourceNotFoundException, если пользователь не найден")
    void findByEmail_shouldReturnResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Mono.empty());

        Mono<UserDto> result = userService.findByEmail("nonexistent@example.com");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException &&
                        throwable.getMessage().contains("User not found with email : 'nonexistent@example.com'"))
                .verify();

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    @DisplayName("createUser должен создавать и возвращать UserDto, если email уникален")
    void createUser_shouldCreateAndReturnUserDto_whenEmailIsUnique() {
        UserDto newUserDto = new UserDto();
        newUserDto.setEmail("newuser@example.com");
        newUserDto.setFirstName("New");
        newUserDto.setLastName("User");
        newUserDto.setActive(false);

        User newUserEntity = new User();
        newUserEntity.setEmail("newuser@example.com");
        newUserEntity.setFirstName("New");
        newUserEntity.setLastName("User");
        newUserEntity.setActive(false);

        User savedUserEntity = new User();
        savedUserEntity.setId(2L);
        savedUserEntity.setEmail("newuser@example.com");
        savedUserEntity.setFirstName("New");
        savedUserEntity.setLastName("User");
        savedUserEntity.setActive(false);
        savedUserEntity.setCreatedAt(LocalDateTime.now());
        savedUserEntity.setUpdatedAt(LocalDateTime.now());

        UserDto savedUserDto = new UserDto();
        savedUserDto.setId(2L);
        savedUserDto.setEmail("newuser@example.com");
        savedUserDto.setFirstName("New");
        savedUserDto.setLastName("User");
        savedUserDto.setActive(false);

        when(userMapper.toEntity(any(UserDto.class))).thenReturn(newUserEntity);
        when(userRepository.findByEmail(anyString())).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUserEntity));
        when(userMapper.toDto(any(User.class))).thenReturn(savedUserDto);

        Mono<UserDto> result = userService.createUser(newUserDto);

        StepVerifier.create(result)
                .expectNext(savedUserDto)
                .verifyComplete();

        verify(userMapper).toEntity(newUserDto);
        verify(userRepository).findByEmail(newUserDto.getEmail());
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDto(savedUserEntity);
    }
    @Test
    @DisplayName("createUser должен создавать и возвращать UserDto с активным true по умолчанию")
    void createUser_shouldCreateAndReturnUserDto_withActiveDefaultTrue() {
        UserDto newUserDto = new UserDto();
        newUserDto.setEmail("defaultactive@example.com");
        newUserDto.setFirstName("Default");
        newUserDto.setLastName("Active");

        User newUserEntity = new User();
        newUserEntity.setEmail("defaultactive@example.com");
        newUserEntity.setFirstName("Default");
        newUserEntity.setLastName("Active");

        User savedUserEntity = new User();
        savedUserEntity.setId(3L);
        savedUserEntity.setEmail("defaultactive@example.com");
        savedUserEntity.setFirstName("Default");
        savedUserEntity.setLastName("Active");
        savedUserEntity.setActive(true);
        savedUserEntity.setCreatedAt(LocalDateTime.now());
        savedUserEntity.setUpdatedAt(LocalDateTime.now());

        UserDto savedUserDto = new UserDto();
        savedUserDto.setId(3L);
        savedUserDto.setEmail("defaultactive@example.com");
        savedUserDto.setFirstName("Default");
        savedUserDto.setLastName("Active");
        savedUserDto.setActive(true);

        when(userMapper.toEntity(any(UserDto.class))).thenReturn(newUserEntity);
        when(userRepository.findByEmail(anyString())).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUserEntity));
        when(userMapper.toDto(any(User.class))).thenReturn(savedUserDto);

        Mono<UserDto> result = userService.createUser(newUserDto);

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getActive() != null && dto.getActive())
                .verifyComplete();

        verify(userMapper).toEntity(newUserDto);
        verify(userRepository).findByEmail(newUserDto.getEmail());
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDto(savedUserEntity);
    }


    @Test
    @DisplayName("createUser должен возвращать DataIntegrityViolationException, если email уже существует")
    void createUser_shouldReturnDataIntegrityViolationException_whenEmailExists() {
        UserDto newUserDto = new UserDto();
        newUserDto.setEmail("test@example.com");

        User newUserEntity = new User();
        newUserEntity.setEmail("test@example.com");

        when(userMapper.toEntity(any(UserDto.class))).thenReturn(newUserEntity);
        when(userRepository.findByEmail(anyString())).thenReturn(Mono.just(userEntity));
        when(userRepository.save(any(User.class))).thenReturn(Mono.empty());


        Mono<UserDto> result = userService.createUser(newUserDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof DataIntegrityViolationException &&
                        throwable.getMessage().contains("Email already exists: test@example.com"))
                .verify();

        verify(userMapper).toEntity(newUserDto);
        verify(userRepository).findByEmail(newUserDto.getEmail());
        verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    @DisplayName("createUser должен возвращать IllegalArgumentException, если email пустой")
    void createUser_shouldReturnIllegalArgumentException_whenEmailIsBlank() {
        UserDto newUserDto = new UserDto();
        newUserDto.setEmail("");

        User newUserEntity = new User();
        newUserEntity.setEmail("");

        when(userMapper.toEntity(any(UserDto.class))).thenReturn(newUserEntity);

        Mono<UserDto> result = userService.createUser(newUserDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Email cannot be blank"))
                .verify();

        verify(userMapper).toEntity(newUserDto);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).toDto(any(User.class));
    }
    @Test
    @DisplayName("createUser должен возвращать IllegalArgumentException, если email null")
    void createUser_shouldReturnIllegalArgumentException_whenEmailIsNull() {
        UserDto newUserDto = new UserDto();
        newUserDto.setEmail(null);

        User newUserEntity = new User();
        newUserEntity.setEmail(null);

        when(userMapper.toEntity(any(UserDto.class))).thenReturn(newUserEntity);

        Mono<UserDto> result = userService.createUser(newUserDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Email cannot be blank"))
                .verify();

        verify(userMapper).toEntity(newUserDto);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).toDto(any(User.class));
    }


    @Test
    @DisplayName("findUserById должен возвращать UserDto, если пользователь найден")
    void findUserById_shouldReturnUserDto_whenUserFound() {
        when(userRepository.findById(anyLong())).thenReturn(Mono.just(userEntity));
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        Mono<UserDto> result = userService.findUserById(1L);

        StepVerifier.create(result)
                .expectNext(userDto)
                .verifyComplete();

        verify(userRepository).findById(1L);
        verify(userMapper).toDto(userEntity);
    }

    @Test
    @DisplayName("findUserById должен возвращать ResourceNotFoundException, если пользователь не найден")
    void findUserById_shouldReturnResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Mono.empty());

        Mono<UserDto> result = userService.findUserById(99L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException &&
                        throwable.getMessage().contains("User not found with id : '99'"))
                .verify();

        verify(userRepository).findById(99L);
        verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    @DisplayName("updateUser должен обновлять и возвращать UserDto, если пользователь найден")
    void updateUser_shouldUpdateAndReturnUserDto_whenUserFound() {
        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setEmail("updated@example.com");
        updatedUserDto.setFirstName("Updated");
        updatedUserDto.setActive(false); // Устанавливаем активность для проверки

        User existingUserEntity = new User();
        existingUserEntity.setId(1L);
        existingUserEntity.setEmail("test@example.com");
        existingUserEntity.setFirstName("Test");
        existingUserEntity.setLastName("User");
        existingUserEntity.setActive(true); // Изначальная активность

        // Сущность после применения изменений маппером и сохранения
        User savedUserEntity = new User();
        savedUserEntity.setId(1L);
        savedUserEntity.setEmail("updated@example.com");
        savedUserEntity.setFirstName("Updated");
        savedUserEntity.setLastName("User"); // lastName не менялся
        savedUserEntity.setActive(false); // Активность должна измениться
        savedUserEntity.setUpdatedAt(LocalDateTime.now());

        UserDto returnedUserDto = new UserDto();
        returnedUserDto.setId(1L);
        returnedUserDto.setEmail("updated@example.com");
        returnedUserDto.setFirstName("Updated");
        returnedUserDto.setLastName("User");
        returnedUserDto.setActive(false); // Активность должна быть false в возвращаемом DTO
        returnedUserDto.setCreatedAt(userEntity.getCreatedAt());


        when(userRepository.findById(anyLong())).thenReturn(Mono.just(existingUserEntity));
        doNothing().when(userMapper).updateEntityFromDto(any(UserDto.class), any(User.class));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUserEntity));
        when(userMapper.toDto(any(User.class))).thenReturn(returnedUserDto);

        Mono<UserDto> result = userService.updateUser(1L, updatedUserDto);

        StepVerifier.create(result)
                .expectNext(returnedUserDto)
                .verifyComplete();

        verify(userRepository).findById(1L);
        verify(userMapper).updateEntityFromDto(updatedUserDto, existingUserEntity); // Проверка, что маппер вызывался с правильными аргументами
        verify(userRepository).save(existingUserEntity); // Проверка, что save был вызван с модифицированной сущностью
        verify(userMapper).toDto(savedUserEntity);
    }


    @Test
    @DisplayName("updateUser должен возвращать ResourceNotFoundException, если пользователь не найден")
    void updateUser_shouldReturnResourceNotFoundException_whenUserNotFound() {
        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setEmail("updated@example.com");

        when(userRepository.findById(anyLong())).thenReturn(Mono.empty());

        Mono<UserDto> result = userService.updateUser(99L, updatedUserDto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException &&
                        throwable.getMessage().contains("User not found with id : '99'"))
                .verify();

        verify(userRepository).findById(99L);
        verify(userMapper, never()).updateEntityFromDto(any(UserDto.class), any(User.class));
        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    @DisplayName("deleteUserById должен успешно завершаться, если пользователь найден")
    void deleteUserById_shouldCompleteSuccessfully_whenUserFound() {
        when(userRepository.findById(anyLong())).thenReturn(Mono.just(userEntity));
        when(userRepository.deleteById(anyLong())).thenReturn(Mono.empty());

        Mono<Void> result = userService.deleteUserById(1L);

        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository).findById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUserById должен возвращать ResourceNotFoundException, если пользователь не найден")
    void deleteUserById_shouldReturnResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Mono.empty());

        Mono<Void> result = userService.deleteUserById(99L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException &&
                        throwable.getMessage().contains("User not found with id : '99'"))
                .verify();

        verify(userRepository).findById(99L);
        verify(userRepository, never()).deleteById(anyLong());
    }
}