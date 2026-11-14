package com.proprefact.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class HorlogeConfig {

    /** Les factures sont datées à l'heure légale française, quel que soit le fuseau du serveur. */
    @Bean
    Clock horloge() {
        return Clock.system(ZoneId.of("Europe/Paris"));
    }
}
