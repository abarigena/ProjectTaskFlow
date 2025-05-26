package com.abarigena.taskflow.storeSQL.repository;

import com.abarigena.taskflow.storeSQL.entity.Project;
import com.abarigena.taskflow.storeSQL.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProjectRepository extends R2dbcRepository<Project, Long> {

    /**
     * Находит все проекты с поддержкой пагинации и сортировки.
     *
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток всех проектов, соответствующих параметрам пагинации.
     */
    Flux<Project> findAllBy(Pageable pageable);

    /**
     * Находит проекты, принадлежащие указанному владельцу, с поддержкой пагинации и сортировки.
     *
     * @param ownerId  Идентификатор владельца проекта.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток проектов указанного владельца, соответствующих параметрам пагинации.
     */
    Flux<Project> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Находит все проекты, в которых участвует указанный пользователь, с поддержкой пагинации и сортировки.
     * Выполняет соединение через промежуточную таблицу project_users.
     *
     * @param userId   Идентификатор пользователя.
     * @param pageable Параметры пагинации и сортировки.
     * @return Поток проектов, в которых участвует пользователь, соответствующих параметрам пагинации.
     */
    @Query("select p.* from projects p join project_users pu on p.id = pu.project_id" +
            " where pu.user_id = :userId")
    Flux<Project> findProjectsByUserId(Long userId, Pageable pageable);

    /**
     * Добавляет пользователя в проект, создавая запись в промежуточной таблице project_users.
     * При конфликте (пара projectId и userId уже существует) ничего не делает.
     *
     * @param projectId Идентификатор проекта.
     * @param userId    Идентификатор пользователя.
     * @return Пустой Mono, сигнализирующий о завершении операции.
     */
    @Query("insert into project_users (project_id, user_id) values (:projectId, :userId) ON CONFLICT DO NOTHING")
    Mono<Void> addUserToProject(Long projectId, Long userId);

    /**
     * Удаляет пользователя из проекта, удаляя запись из промежуточной таблицы project_users.
     *
     * @param projectId Идентификатор проекта.
     * @param userId    Идентификатор пользователя.
     * @return Пустой Mono, сигнализирующий о завершении операции.
     */
    @Query("delete from project_users where project_id = :projectId and user_id = :userId")
    Mono<Void> deleteUserFromProject(Long projectId, Long userId);

    /**
     * Находит всех пользователей, которые являются участниками указанного проекта.
     * Выполняет соединение через промежуточную таблицу project_users.
     *
     * @param projectId Идентификатор проекта.
     * @return Поток пользователей, являющихся участниками проекта.
     */
    @Query("select u.* from users u " +
            "join project_users pu on u.id = pu.user_id " +
            "where pu.project_id = :projectId")
    Flux<User> findUsersInProject(Long projectId);

}
