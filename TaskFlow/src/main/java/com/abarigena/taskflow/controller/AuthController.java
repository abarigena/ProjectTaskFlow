package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.dto.auth.AuthResponse;
import com.abarigena.taskflow.dto.auth.LoginRequest;
import com.abarigena.taskflow.dto.auth.RefreshTokenRequest;
import com.abarigena.taskflow.dto.auth.RegisterRequest;
import com.abarigena.taskflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/test")
    public Mono<ResponseEntity<String>> test(@RequestBody RegisterRequest request) {
        log.info("Test endpoint received: {}", request);
        return Mono.just(ResponseEntity.ok("Received: " + request.toString()));
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request != null ? request.getEmail() : "null request");
        log.debug("Full registration request: {}", request);
        return authService.register(request)
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("Registration failed", error))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request != null ? request.getEmail() : "null request");
        log.debug("Full login request: {}", request);
        return authService.login(request)
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("Login failed", error))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token attempt");
        log.debug("Refresh token request: {}", request);
        return authService.refreshToken(request)
                .map(ResponseEntity::ok)
                .doOnError(error -> log.error("Token refresh failed", error))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }
} 