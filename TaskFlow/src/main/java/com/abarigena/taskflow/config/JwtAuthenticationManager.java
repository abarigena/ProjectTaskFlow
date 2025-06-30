package com.abarigena.taskflow.config;

import com.abarigena.taskflow.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Primary
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;
    private final ReactiveUserDetailsService userDetailsService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication)
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuthenticationToken -> {
                    String token = (String) jwtAuthenticationToken.getCredentials();
                    String username = jwtService.extractUsername(token);
                    
                    return userDetailsService.findByUsername(username)
                            .filter(userDetails -> jwtService.isTokenValid(token, userDetails))
                            .map(userDetails -> new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            ));
                });
    }
} 