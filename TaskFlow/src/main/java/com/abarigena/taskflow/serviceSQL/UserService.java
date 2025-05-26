package com.abarigena.taskflow.serviceSQL;

import com.abarigena.taskflow.dto.UserDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<UserDto> findAllUsers(Pageable pageable);
    Mono<UserDto> findByEmail(String email);
    Mono<UserDto> createUser(UserDto userDto);
    Mono<UserDto> findUserById(Long id);
    Mono<UserDto> updateUser(Long id, UserDto userDto);
    Mono<Void> deleteUserById(Long id);
}
