package com.proprefact.config;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.proprefact.service.DepotService;

/** Retente chaque minute les dépôts restés en cours. Désactivable avec app.depot.relance-active=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.depot.relance-active", havingValue = "true", matchIfMissing = true)
class RelanceDepots {
    private final DepotService depots;

    RelanceDepots(DepotService depots) {
        this.depots = depots;
    }

    @Scheduled(fixedDelayString = "${app.depot.relance-ms:60000}", initialDelayString = "${app.depot.relance-ms:60000}")
    void relancer() {
        depots.relancer(Instant.now());
    }
}
