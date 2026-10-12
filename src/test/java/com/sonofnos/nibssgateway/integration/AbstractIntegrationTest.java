package com.sonofnos.nibssgateway.integration;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Deliberately does NOT use JUnit 5's {@code @Testcontainers}/{@code
 * @Container} lifecycle management: with multiple integration test
 * classes in the same Maven Surefire run, that per-class lifecycle has
 * previously started a fresh Postgres container per test class and torn
 * the previous one down mid-suite, leaving other classes' Hikari
 * connections pointing at a container that no longer existed. Starting
 * the container exactly once, eagerly, in a static initializer (the
 * documented Testcontainers "singleton container" pattern) means it
 * lives for the whole JVM/Surefire fork. Same fix as
 * payments-collections-service and core-banking-service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("nibss")
                .withUsername("nibss")
                .withPassword("nibss");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
