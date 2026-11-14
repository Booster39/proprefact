package com.proprefact.config;

import com.proprefact.service.DepotService;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Retente chaque minute les dépôts restés en cours. Désactivable avec app.depot.relance-active=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.depot.relance-active", havingValue = "true", matchIfMissing = true)
class RelanceDepots {
    private final DepotService depots;
    private final Clock horloge;

    RelanceDepots(DepotService depots, Clock horloge) {
        this.depots = depots;
        this.horloge = horloge;
    }

    @Scheduled(fixedDelayString = "${app.depot.relance-ms:60000}", initialDelayString = "${app.depot.relance-ms:60000}")
    void relancer() {
        depots.relancer(horloge.instant());
    }
}
