package com.proprefact.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

/** Lecture des réglages de la base dans application.properties : DB_* d'abord, sinon celles de Clever Cloud. */
class ConnexionBaseTest {

    @Test
    void lesVariablesDeCleverCloudSuffisent() throws IOException {
        StandardEnvironment environnement = environnement(Map.of(
                "POSTGRESQL_ADDON_HOST", "bxyz-postgresql.services.clever-cloud.com",
                "POSTGRESQL_ADDON_PORT", "50013",
                "POSTGRESQL_ADDON_DB", "bxyz",
                "POSTGRESQL_ADDON_USER", "uxyz",
                "POSTGRESQL_ADDON_PASSWORD", "secret-de-test"));

        assertThat(environnement.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://bxyz-postgresql.services.clever-cloud.com:50013/bxyz");
        assertThat(environnement.getProperty("spring.datasource.username")).isEqualTo("uxyz");
        assertThat(environnement.getProperty("spring.datasource.password")).isEqualTo("secret-de-test");
    }

    @Test
    void lesVariablesDbPassentAvant() throws IOException {
        StandardEnvironment environnement = environnement(Map.of(
                "DB_URL", "jdbc:postgresql://db:5432/proprefact",
                "DB_USER", "proprefact",
                "DB_PASSWORD", "autre-secret",
                "POSTGRESQL_ADDON_HOST", "ignore",
                "POSTGRESQL_ADDON_DB", "ignore",
                "POSTGRESQL_ADDON_USER", "ignore",
                "POSTGRESQL_ADDON_PASSWORD", "ignore"));

        assertThat(environnement.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db:5432/proprefact");
        assertThat(environnement.getProperty("spring.datasource.username")).isEqualTo("proprefact");
        assertThat(environnement.getProperty("spring.datasource.password")).isEqualTo("autre-secret");
    }

    @Test
    void sansAucuneVariableLaBaseNEstPasDevinee() throws IOException {
        StandardEnvironment environnement = environnement(Map.of());

        assertThatThrownBy(() -> environnement.getProperty("spring.datasource.url"))
                .hasMessageContaining("POSTGRESQL_ADDON_HOST");
        assertThatThrownBy(() -> environnement.getProperty("spring.datasource.password"))
                .hasMessageContaining("POSTGRESQL_ADDON_PASSWORD");
    }

    /** application.properties seul, avec les variables données à la place de celles de la machine. */
    private static StandardEnvironment environnement(Map<String, Object> variables) throws IOException {
        StandardEnvironment environnement = new StandardEnvironment();
        environnement.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environnement.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environnement.getPropertySources().addFirst(new MapPropertySource("variables", variables));
        environnement.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        return environnement;
    }
}
