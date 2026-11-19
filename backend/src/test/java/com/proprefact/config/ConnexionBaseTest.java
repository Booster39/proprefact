package com.proprefact.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

/** Lecture des réglages de la base dans application.properties : DB_* d'abord, sinon les variables PG* de Railway. */
class ConnexionBaseTest {

    @Test
    void lesVariablesDeRailwaySuffisent() throws IOException {
        StandardEnvironment environnement = environnement(Map.of(
                "PGHOST", "postgres.railway.internal",
                "PGPORT", "5432",
                "PGDATABASE", "railway",
                "PGUSER", "postgres",
                "PGPASSWORD", "secret-de-test"));

        assertThat(environnement.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://postgres.railway.internal:5432/railway");
        assertThat(environnement.getProperty("spring.datasource.username")).isEqualTo("postgres");
        assertThat(environnement.getProperty("spring.datasource.password")).isEqualTo("secret-de-test");
    }

    @Test
    void lesVariablesDbPassentAvant() throws IOException {
        StandardEnvironment environnement = environnement(Map.of(
                "DB_URL", "jdbc:postgresql://db:5432/proprefact",
                "DB_USER", "proprefact",
                "DB_PASSWORD", "autre-secret",
                "PGHOST", "ignore",
                "PGDATABASE", "ignore",
                "PGUSER", "ignore",
                "PGPASSWORD", "ignore"));

        assertThat(environnement.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db:5432/proprefact");
        assertThat(environnement.getProperty("spring.datasource.username")).isEqualTo("proprefact");
        assertThat(environnement.getProperty("spring.datasource.password")).isEqualTo("autre-secret");
    }

    @Test
    void sansAucuneVariableLaBaseNEstPasDevinee() throws IOException {
        StandardEnvironment environnement = environnement(Map.of());

        assertThatThrownBy(() -> environnement.getProperty("spring.datasource.url"))
                .hasMessageContaining("PGHOST");
        assertThatThrownBy(() -> environnement.getProperty("spring.datasource.password"))
                .hasMessageContaining("PGPASSWORD");
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
