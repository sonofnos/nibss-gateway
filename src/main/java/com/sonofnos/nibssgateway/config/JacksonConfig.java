package com.sonofnos.nibssgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4.1's own auto-configured ObjectMapper moved to the new
 * tools.jackson (Jackson 3) engine, but springdoc-openapi still pulls
 * in classic com.fasterxml.jackson:jackson-databind (2.x), and nothing
 * wires a classic ObjectMapper as a bean any more. WebhookController
 * parses raw webhook bodies with the classic ObjectMapper explicitly,
 * so it needs this bean. Same issue and fix as payments-collections-service.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
