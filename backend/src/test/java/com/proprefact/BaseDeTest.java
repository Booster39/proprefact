package com.proprefact;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Base PostgreSQL jetable, identique à celle de production, démarrée pour les tests d'intégration. */
@TestConfiguration(proxyBeanMethods = false)
public class BaseDeTest {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16");
    }
}
