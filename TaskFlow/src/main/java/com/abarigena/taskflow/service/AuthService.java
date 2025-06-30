package com.abarigena.taskflow.service;

import com.abarigena.taskflow.dto.auth.AuthResponse;
import com.abarigena.taskflow.dto.auth.LoginRequest;
import com.abarigena.taskflow.dto.auth.RefreshTokenRequest;
import com.abarigena.taskflow.dto.auth.RegisterRequest;
import com.abarigena.taskflow.storeSQL.entity.Role;
import com.abarigena.taskflow.storeSQL.entity.User;
import com.abarigena.taskflow.storeSQL.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ReactiveUserDetailsService userDetailsService;

    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("User with this email already exists"));
                    }
                    
                    User user = User.builder()
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .role(Role.USER)
                            .active(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return userRepository.save(user)
                            .flatMap(savedUser -> 
                                userDetailsService.findByUsername(savedUser.getEmail())
                                    .map(userDetails -> {
                                        String token = jwtService.generateToken(userDetails, savedUser.getId());
                                        String refreshToken = jwtService.generateRefreshToken(userDetails, savedUser.getId());
                                        return AuthResponse.builder()
                                                .token(token)
                                                .refreshToken(refreshToken)
                                                .email(savedUser.getEmail())
                                                .role(savedUser.getRole().name())
                                                .build();
                                    }));
                });
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new RuntimeException("Invalid credentials"));
                    }
                    
                    return userDetailsService.findByUsername(user.getEmail())
                            .map(userDetails -> {
                                String token = jwtService.generateToken(userDetails, user.getId());
                                String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
                                return AuthResponse.builder()
                                        .token(token)
                                        .refreshToken(refreshToken)
                                        .email(user.getEmail())
                                        .role(user.getRole().name())
                                        .build();
                            });
                });
    }

    public Mono<AuthResponse> refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        return Mono.fromCallable(() -> jwtService.extractUsername(refreshToken))
        .flatMap(username -> userDetailsService.findByUsername(username)
                .filter(userDetails -> jwtService.isRefreshTokenValid(refreshToken, userDetails))
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid refresh token")))
                .flatMap(userDetails -> 
                    userRepository.findByEmail(userDetails.getUsername())
                        .map(user -> {
                            String newToken = jwtService.generateToken(userDetails, user.getId());
                            String newRefreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
                            return AuthResponse.builder()
                                    .token(newToken)
                                    .refreshToken(newRefreshToken)
                                    .email(user.getEmail())
                                    .role(user.getRole().name())
                                    .build();
                        })
                )
        )
        .onErrorMap(throwable -> new RuntimeException("Failed to refresh token: " + throwable.getMessage()));
    }
} 