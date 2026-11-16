package com.proprefact;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Remplace la messagerie et la plateforme agréée par des doubles que les tests pilotent et relisent. */
@TestConfiguration(proxyBeanMethods = false)
class DoublesDeTest {
    @Bean
    @Primary
    PlateformeDeTest plateformeDeTest() {
        return new PlateformeDeTest();
    }

    @Bean
    @Primary
    Courrier courrier() {
        return new Courrier();
    }
}
