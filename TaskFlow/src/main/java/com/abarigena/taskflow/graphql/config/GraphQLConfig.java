package com.abarigena.taskflow.graphql.config;

import com.abarigena.taskflow.graphql.scalar.LocalDateTimeScalar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Конфигурация GraphQL для регистрации кастомных скаляров и других настроек.
 */
@Configuration
public class GraphQLConfig {

    /**
     * Конфигурирует RuntimeWiring для регистрации кастомных скаляров.
     * В данном случае регистрируем LocalDateTime скаляр.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(LocalDateTimeScalar.INSTANCE);
    }
} 