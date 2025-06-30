package com.abarigena.taskflow.utility;

import com.abarigena.taskflow.config.JwtAuthenticationToken;
import com.abarigena.taskflow.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Утилитный класс для работы с контекстом безопасности
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final JwtService jwtService;

    /**
     * Получает ID текущего аутентифицированного пользователя из JWT токена
     * @return Mono с ID пользователя или пустой Mono, если пользователь не аутентифицирован
     */
    public Mono<Long> getCurrentUserId() {
        return Mono.deferContextual(contextView -> {
                return Mono.justOrEmpty(contextView.getOrEmpty(ServerWebExchange.class))
                    .cast(ServerWebExchange.class)
                    .flatMap(this::extractUserIdFromRequest)
                    .switchIfEmpty(
                            // Fallback к старому методу через SecurityContext
                            ReactiveSecurityContextHolder.getContext()
                                    .map(context -> context.getAuthentication())
                                    .filter(Authentication::isAuthenticated)
                                    .flatMap(authentication -> {
                                        log.debug("Fallback: аутентификация через SecurityContext");
                                        return Mono.empty();
                                    })
                    );
        })
        .doOnNext(userId -> log.debug("Получен ID текущего пользователя из JWT: {}", userId))
        .onErrorResume(throwable -> {
            log.warn("Не удалось получить ID текущего пользователя: {}", throwable.getMessage());
            return Mono.empty();
        });
    }
    
    /**
     * Извлекает ID пользователя из JWT токена в заголовке Authorization
     */
    private Mono<Long> extractUserIdFromRequest(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Long userId = jwtService.extractUserId(token);
                if (userId != null) {
                    return Mono.just(userId);
                }
            } catch (Exception e) {
                log.debug("Ошибка извлечения userId из токена: {}", e.getMessage());
            }
        }
        return Mono.empty();
    }

    /**
     * Получает email текущего аутентифицированного пользователя из JWT токена
     * @return Mono с email пользователя или пустой Mono, если пользователь не аутентифицирован
     */
    public Mono<String> getCurrentUserEmail() {
        return Mono.deferContextual(contextView -> {
            return Mono.justOrEmpty(contextView.getOrEmpty(ServerWebExchange.class))
                    .cast(ServerWebExchange.class)
                    .flatMap(this::extractEmailFromRequest)
                    .switchIfEmpty(
                            // Fallback к SecurityContext
                            ReactiveSecurityContextHolder.getContext()
                                    .map(context -> context.getAuthentication())
                                    .filter(Authentication::isAuthenticated)
                                    .cast(Authentication.class)
                                    .map(Authentication::getPrincipal)
                                    .cast(UserDetails.class)
                                    .map(UserDetails::getUsername) // это email
                    );
        })
        .doOnNext(email -> log.debug("Получен email текущего пользователя: {}", email))
        .onErrorResume(throwable -> {
            log.warn("Не удалось получить email текущего пользователя: {}", throwable.getMessage());
            return Mono.empty();
        });
    }
    
    /**
     * Извлекает email пользователя из JWT токена в заголовке Authorization
     */
    private Mono<String> extractEmailFromRequest(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtService.extractUsername(token);
                if (email != null) {
                    return Mono.just(email);
                }
            } catch (Exception e) {
                log.debug("Ошибка извлечения email из токена: {}", e.getMessage());
            }
        }
        return Mono.empty();
    }
} 