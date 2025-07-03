package com.abarigena.taskflow.task15.service;

import com.abarigena.taskflow.task15.dto.Task15UserDto;
import com.abarigena.taskflow.task15.types.UserRole;
import com.abarigena.taskflow.task15.types.UserAccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDateTime;
import java.util.List;

import static com.abarigena.taskflow.task15.jooq.Tables.TASK15_USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class Task15UserServiceImpl implements Task15UserService {

    @Qualifier("task15DSLContext")
    private final DSLContext dsl;
    
    @Qualifier("jdbcScheduler")
    private final Scheduler jdbcScheduler;

    @Override
    public Mono<Task15UserDto> findById(Long id) {
        log.debug("Finding user by id: {}", id);
        
        return Mono.fromCallable(() -> {
            log.debug("Executing JOOQ query in thread: {}", Thread.currentThread().getName());
            
            var record = dsl.selectFrom(TASK15_USER)
                    .where(TASK15_USER.ID.eq(id))
                    .fetchOne();
                    
            return record != null ? mapToDto(record) : null;
        })
        .subscribeOn(jdbcScheduler)  
        .doOnSuccess(user -> log.debug("Found user: {}", user))
        .doOnError(error -> log.error("Error finding user by id {}: {}", id, error.getMessage()));
    }

    @Override
    public Flux<Task15UserDto> findAll() {
        log.debug("Finding all users");
        
        return Mono.fromCallable(() -> {
            log.debug("Executing JOOQ findAll in thread: {}", Thread.currentThread().getName());
            
            List<Task15UserDto> users = dsl.selectFrom(TASK15_USER)
                    .orderBy(TASK15_USER.DATE_CREATED.desc())
                    .fetch()
                    .map(this::mapToDto);
                    
            return users;
        })
        .subscribeOn(jdbcScheduler)  
        .flatMapMany(Flux::fromIterable)  // Преобразуем List в Flux
        .doOnNext(user -> log.debug("Emitting user: {}", user.getUsername()));
    }

    @Override
    public Flux<Task15UserDto> findByRole(UserRole role) {
        log.debug("Finding users by role: {}", role);
        
        return Mono.fromCallable(() -> {
            var users = dsl.selectFrom(TASK15_USER)
                    .where(TASK15_USER.ROLE.eq(role))  // UserRole
                    .orderBy(TASK15_USER.USERNAME.asc())
                    .fetch()
                    .map(this::mapToDto);
                    
            return users;
        })
        .subscribeOn(jdbcScheduler)
        .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<Task15UserDto> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        
        return Mono.fromCallable(() -> {
            var record = dsl.selectFrom(TASK15_USER)
                    .where(TASK15_USER.USERNAME.eq(username))
                    .fetchOne();
                    
            return record != null ? mapToDto(record) : null;
        })
        .subscribeOn(jdbcScheduler);
    }

    @Override
    public Mono<Task15UserDto> create(Task15UserDto userDto) {
        log.debug("Creating user: {}", userDto.getUsername());
        
        return Mono.fromCallable(() -> {
            log.debug("Executing JOOQ insert in thread: {}", Thread.currentThread().getName());
            
            var record = dsl.insertInto(TASK15_USER)
                    .set(TASK15_USER.USERNAME, userDto.getUsername())
                    .set(TASK15_USER.EMAIL, userDto.getEmail())
                    .set(TASK15_USER.FULL_NAME, userDto.getFullName())
                    .set(TASK15_USER.ROLE, userDto.getRole())  // Автоматический enum -> String
                    .set(TASK15_USER.ACCOUNT_STATUS, 
                         userDto.getAccountStatus() != null ? 
                         userDto.getAccountStatus() : UserAccountStatus.NOT_VERIFIED)
                    .returning()  // Возвращаем созданную запись
                    .fetchOne();
                    
            if (record == null) {
                throw new RuntimeException("Failed to create user");
            }
            
            return mapToDto(record);
        })
        .subscribeOn(jdbcScheduler)
        .doOnSuccess(created -> log.info("Created user with id: {}", created.getId()));
    }

    @Override
    public Mono<Task15UserDto> update(Long id, Task15UserDto userDto) {
        log.debug("Updating user id: {}", id);
        
        return Mono.fromCallable(() -> {
            var record = dsl.update(TASK15_USER)
                    .set(TASK15_USER.USERNAME, userDto.getUsername())
                    .set(TASK15_USER.EMAIL, userDto.getEmail())
                    .set(TASK15_USER.FULL_NAME, userDto.getFullName())
                    .set(TASK15_USER.ROLE, userDto.getRole())
                    .set(TASK15_USER.ACCOUNT_STATUS, userDto.getAccountStatus())
                    .set(TASK15_USER.DATE_UPDATED, LocalDateTime.now())
                    .where(TASK15_USER.ID.eq(id))
                    .returning()
                    .fetchOne();
                    
            if (record == null) {
                throw new RuntimeException("User not found or update failed");
            }
            
            return mapToDto(record);
        })
        .subscribeOn(jdbcScheduler)
        .doOnSuccess(updated -> log.info("Updated user: {}", updated.getUsername()));
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        log.debug("Deleting user by id: {}", id);
        
        return Mono.fromCallable(() -> {
            int deleted = dsl.deleteFrom(TASK15_USER)
                    .where(TASK15_USER.ID.eq(id))
                    .execute();
                    
            if (deleted == 0) {
                throw new RuntimeException("User not found");
            }
            
            return deleted;
        })
        .subscribeOn(jdbcScheduler)
        .then()  // Преобразуем в Mono<Void>
        .doOnSuccess(v -> log.info("Deleted user with id: {}", id));
    }

    @Override
    public Mono<Boolean> existsByUsername(String username) {
        return Mono.fromCallable(() -> {
            // fetchExists() - оптимизированный метод для проверки существования
            return dsl.fetchExists(
                dsl.selectFrom(TASK15_USER)
                   .where(TASK15_USER.USERNAME.eq(username))
            );
        })
        .subscribeOn(jdbcScheduler);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return Mono.fromCallable(() -> {
            return dsl.fetchExists(
                dsl.selectFrom(TASK15_USER)
                   .where(TASK15_USER.EMAIL.eq(email))
            );
        })
        .subscribeOn(jdbcScheduler);
    }

    /**
     * Маппер из JOOQ Record в DTO
     */
    private Task15UserDto mapToDto(org.jooq.Record record) {
        Task15UserDto dto = new Task15UserDto();
        dto.setId(record.get(TASK15_USER.ID));
        dto.setUsername(record.get(TASK15_USER.USERNAME));
        dto.setEmail(record.get(TASK15_USER.EMAIL));
        dto.setFullName(record.get(TASK15_USER.FULL_NAME));
        dto.setRole(record.get(TASK15_USER.ROLE));  // String -> UserRole
        dto.setAccountStatus(record.get(TASK15_USER.ACCOUNT_STATUS));  // String -> UserAccountStatus
        dto.setDateCreated(record.get(TASK15_USER.DATE_CREATED));
        dto.setDateUpdated(record.get(TASK15_USER.DATE_UPDATED));
        return dto;
    }
} 