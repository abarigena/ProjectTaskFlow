package com.abarigena.taskflow.graphql.scalar;

import graphql.GraphQLError;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Кастомный GraphQL скаляр для работы с LocalDateTime.
 * Конвертирует LocalDateTime в ISO-8601 
 */
@Slf4j
public class LocalDateTimeScalar {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("LocalDateTime")
            .description("Локальная дата и время в формате ISO-8601 (например: 2023-12-25T14:30:00)")
            .coercing(new Coercing<LocalDateTime, String>() {

                /**
                 * Конвертирует LocalDateTime в String для отправки клиенту
                 */
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof LocalDateTime localDateTime) {
                        return localDateTime.format(FORMATTER);
                    }
                    
                    log.error("Cannot serialize object as LocalDateTime: {}", dataFetcherResult);
                    throw new CoercingSerializeException("Expected LocalDateTime but got: " + dataFetcherResult.getClass());
                }

                /**
                 * Конвертирует входящий String в LocalDateTime (из переменных запроса)
                 */
                @Override
                public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof String stringInput) {
                        try {
                            return LocalDateTime.parse(stringInput, FORMATTER);
                        } catch (DateTimeParseException e) {
                            log.error("Cannot parse '{}' as LocalDateTime: {}", stringInput, e.getMessage());
                            throw new CoercingParseValueException("Cannot parse '" + stringInput + "' as LocalDateTime", e);
                        }
                    }
                    
                    log.error("Cannot parse value as LocalDateTime: {}", input);
                    throw new CoercingParseValueException("Expected String but got: " + input.getClass());
                }

                /**
                 * Конвертирует литерал из GraphQL запроса в LocalDateTime
                 */
                @Override
                public LocalDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue stringValue) {
                        try {
                            return LocalDateTime.parse(stringValue.getValue(), FORMATTER);
                        } catch (DateTimeParseException e) {
                            log.error("Cannot parse literal '{}' as LocalDateTime: {}", stringValue.getValue(), e.getMessage());
                            throw new CoercingParseLiteralException("Cannot parse '" + stringValue.getValue() + "' as LocalDateTime", e);
                        }
                    }
                    
                    log.error("Cannot parse literal as LocalDateTime: {}", input);
                    throw new CoercingParseLiteralException("Expected StringValue but got: " + input.getClass());
                }
            })
            .build();
} 