package com.proprefact.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Compte des tentatives par clé (adresse IP, e-mail...) sur une fenêtre de temps fixe.
 * Les compteurs sont en mémoire : ils sont propres à chaque instance et repartent de zéro au redémarrage.
 */
@Component
public class LimiteurTentatives {
    static final int TAILLE_AVANT_NETTOYAGE = 10_000;

    private record Compteur(Instant debut, int nombre) {}

    private final ConcurrentHashMap<String, Compteur> compteurs = new ConcurrentHashMap<>();
    private final Clock horloge;

    public LimiteurTentatives() {
        this(Clock.systemUTC());
    }

    LimiteurTentatives(Clock horloge) {
        this.horloge = horloge;
    }

    /** Enregistre une tentative et indique si elle reste dans la limite. */
    public boolean tenter(String cle, int maximum, Duration fenetre) {
        Instant maintenant = horloge.instant();
        if (compteurs.size() > TAILLE_AVANT_NETTOYAGE) {
            compteurs.values().removeIf(c -> expire(c, maintenant, fenetre));
        }
        Compteur resultat = compteurs.compute(cle, (k, c) -> c == null || expire(c, maintenant, fenetre)
                ? new Compteur(maintenant, 1)
                : new Compteur(c.debut(), c.nombre() + 1));
        return resultat.nombre() <= maximum;
    }

    /** Indique si la limite est déjà atteinte, sans compter de tentative. */
    public boolean atteinte(String cle, int maximum, Duration fenetre) {
        Compteur c = compteurs.get(cle);
        return c != null && !expire(c, horloge.instant(), fenetre) && c.nombre() >= maximum;
    }

    public void oublier(String cle) {
        compteurs.remove(cle);
    }

    private static boolean expire(Compteur c, Instant maintenant, Duration fenetre) {
        return !c.debut().plus(fenetre).isAfter(maintenant);
    }
}
