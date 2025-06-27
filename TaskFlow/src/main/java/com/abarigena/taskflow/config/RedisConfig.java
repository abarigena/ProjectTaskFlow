package com.abarigena.taskflow.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Настройка ObjectMapper для корректной сериализации/десериализации объектов в Redis
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Добавляем поддержку Java Time API (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        
        // Включаем информацию о типах для правильной десериализации
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        return mapper;
    }

    /**
     * Реактивный RedisTemplate для прямой работы с Redis
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {
        
        // Настраиваем сериализаторы
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // Создаём контекст сериализации
        RedisSerializationContext<String, Object> serializationContext = 
            RedisSerializationContext.<String, Object>newSerializationContext()
                .key(stringSerializer)
                .hashKey(stringSerializer)
                .value(jsonSerializer)
                .hashValue(jsonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    /**
     * Менеджер кэша с настройками для разных типов данных
     * Для реактивного приложения нам нужен обычный RedisConnectionFactory
     */
    @Bean
    public RedisCacheManager cacheManager(ReactiveRedisConnectionFactory reactiveConnectionFactory,
                                        ObjectMapper redisObjectMapper) {
        
        // Получаем обычную connection factory из реактивной
        // Это нужно для RedisCacheManager, который не поддерживает реактивные соединения
        org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory = 
            ((org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) reactiveConnectionFactory);
        
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // Базовая конфигурация кэша
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))  // TTL по умолчанию - 1 час
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues();

        // Специфичные настройки для разных типов кэша
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Кэш для пользователей - долгоживущий (4 часа)
        cacheConfigurations.put("users", defaultConfig
            .entryTtl(Duration.ofHours(4))
            .prefixCacheNameWith("taskflow:users:"));
        
        // Кэш для проектов - среднеживущий (2 часа)
        cacheConfigurations.put("projects", defaultConfig
            .entryTtl(Duration.ofHours(2))
            .prefixCacheNameWith("taskflow:projects:"));
        
        // Кэш для задач - короткоживущий (30 минут, так как часто обновляются)
        cacheConfigurations.put("tasks", defaultConfig
            .entryTtl(Duration.ofMinutes(30))
            .prefixCacheNameWith("taskflow:tasks:"));
        
        // Кэш для комментариев - короткоживущий (15 минут)
        cacheConfigurations.put("comments", defaultConfig
            .entryTtl(Duration.ofMinutes(15))
            .prefixCacheNameWith("taskflow:comments:"));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
} 