package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.dto.UserDto;
import com.abarigena.taskflow.serviceSQL.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @GetMapping
    Flux<UserDto> findAllUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        log.info("Finding all users");

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        return userService.findAllUsers(pageable);
    }

    @GetMapping("/email")
    Mono<UserDto> getUserByEmail(@RequestParam(value = "email") String email) {
        log.info("Request received for getting user by email: {}", email);

        return userService.findByEmail(email);
    }

    @PostMapping
    Mono<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Request received for creating user: {}", userDto);
        return userService.createUser(userDto);
    }

    @GetMapping("/{id}")
    Mono<UserDto> findUserById(@PathVariable Long id) {
        log.info("Request received for getting user by id: {}", id);

        return userService.findUserById(id);
    }

    @PutMapping("/{id}")
    Mono<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        log.info("Request received for updating user: {}", userDto);

        return userService.updateUser(id, userDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Void> deleteUserById(@PathVariable Long id) {
        log.info("Request received for deleting user by id: {}", id);
        return userService.deleteUserById(id);
    }

}
