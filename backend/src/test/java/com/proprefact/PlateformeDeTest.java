package com.proprefact;

import com.proprefact.service.PlateformeAgreee;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Plateforme agréée de test : idempotente comme l'exige le contrat, avec des pannes programmables. */
class PlateformeDeTest implements PlateformeAgreee {
    final Map<String, String> depotsParCle = new ConcurrentHashMap<>();
    final List<String> clesRecues = new CopyOnWriteArrayList<>();
    volatile int pannesAvantEnvoi;
    volatile int pannesApresEnvoi;

    @Override
    public String deposer(String cleIdempotence, String numeroFacture, byte[] facturX) {
        clesRecues.add(cleIdempotence);
        if (pannesAvantEnvoi > 0) {
            pannesAvantEnvoi--;
            throw new IllegalStateException("Plateforme injoignable");
        }
        String id = depotsParCle.computeIfAbsent(cleIdempotence, cle -> "PA-" + (depotsParCle.size() + 1));
        if (pannesApresEnvoi > 0) {
            pannesApresEnvoi--;
            throw new IllegalStateException("Réponse perdue");
        }
        return id;
    }
}
