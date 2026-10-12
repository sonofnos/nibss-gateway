package com.sonofnos.nibssgateway.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Spring Boot 4 dropped the built-in Flyway auto-configuration module, so
 * migrations are run explicitly here, before the DataSource is handed to
 * JPA. The datasource bean below runs flyway.migrate() as it is built,
 * which guarantees migrations complete before Hibernate's schema
 * validation runs against it. Same fix as payments-collections-service.
 */
@Configuration
public class FlywayConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        return dataSource;
    }
}
