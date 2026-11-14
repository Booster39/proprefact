package com.proprefact.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implémentation provisoire : ne transmet rien. À remplacer par l'appel à l'API de la plateforme
 * agréée retenue avant toute mise en production.
 */
@Component
public class PlateformeAgreeeSimulee implements PlateformeAgreee {
    private static final Logger log = LoggerFactory.getLogger(PlateformeAgreeeSimulee.class);

    private final Map<String, String> depots = new ConcurrentHashMap<>();

    @Override
    public String deposer(String cleIdempotence, String numeroFacture, byte[] facturX) {
        return depots.computeIfAbsent(cleIdempotence, cle -> {
            String id = "SIMU-" + UUID.randomUUID();
            log.warn(
                    "Dépôt SIMULÉ de la facture {} ({} octets) : aucune transmission réelle, id {}",
                    numeroFacture,
                    facturX.length,
                    id);
            return id;
        });
    }
}
