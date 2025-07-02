package com.abarigena.taskflow.task15.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.util.concurrent.Executors;

@Configuration
public class Task15JooqConfig {

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;
    
    @Value("${spring.r2dbc.username}")
    private String username;
    
    @Value("${spring.r2dbc.password}")
    private String password;

    /**
     * Отдельный DataSource для JOOQ (блокирующий JDBC)
     * Конвертируем R2DBC URL в JDBC URL
     */
    @Bean("task15DataSource")
    public DataSource task15DataSource() {
        HikariConfig config = new HikariConfig();
        
        // Конвертируем r2dbc:postgresql в jdbc:postgresql
        String jdbcUrl = r2dbcUrl.replace("r2dbc:postgresql://", "jdbc:postgresql://");
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Настройки пула соединений для JOOQ
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }

    /**
     * DSLContext для JOOQ
     */
    @Bean("task15DSLContext")
    public DSLContext task15DSLContext() {
        return DSL.using(task15DataSource(), SQLDialect.POSTGRES);
    }

    /**
     * Scheduler для выполнения блокирующих JOOQ операций
     * в отдельном пуле потоков
     */
    @Bean("jdbcScheduler")
    public Scheduler jdbcScheduler() {
        return Schedulers.fromExecutor(
            Executors.newFixedThreadPool(
                8, // Размер пула потоков
                r -> {
                    Thread thread = new Thread(r, "jdbc-scheduler");
                    thread.setDaemon(true);
                    return thread;
                }
            )
        );
    }

    /**
     * Transaction Manager для JOOQ операций
     */
    @Bean("task15TransactionManager")
    public DataSourceTransactionManager task15TransactionManager() {
        return new DataSourceTransactionManager(task15DataSource());
    }
} 