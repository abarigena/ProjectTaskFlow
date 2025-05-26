package com.abarigena.taskflow.storeSQL.repository;

import com.abarigena.taskflow.storeSQL.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    /**
     * Находит всех пользователей с поддержкой пагинации и сортировки.
     *
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток всех пользователей, соответствующих параметрам пагинации.
     */
    Flux<User> findAllBy(Pageable pageable);

    /**
     * Находит пользователя по его адресу электронной почты.
     *
     * @param email Адрес электронной почты пользователя.
     * @return Mono, содержащий найденного пользователя, или пустой Mono, если пользователь не найден.
     */
    Mono<User> findByEmail(String email);
}
