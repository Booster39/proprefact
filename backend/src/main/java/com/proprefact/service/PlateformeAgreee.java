package com.proprefact.service;

/**
 * Plateforme agréée (PA) par laquelle les factures électroniques doivent transiter.
 * Toute l'intégration avec le partenaire choisi passe par cette interface, pour pouvoir en changer.
 */
public interface PlateformeAgreee {

    /**
     * Dépose une facture Factur-X et renvoie l'identifiant de dépôt attribué par la plateforme.
     *
     * <p>Contrat à respecter par toute implémentation : l'appel peut être rejoué. Deux appels avec la même
     * clé d'idempotence ne créent qu'un seul dépôt et renvoient le même identifiant ; si la plateforme
     * répond « facture déjà déposée », l'implémentation retrouve l'identifiant existant au lieu d'échouer.
     * Toute exception signifie « résultat inconnu » : l'appel sera retenté plus tard avec la même clé.
     */
    String deposer(String cleIdempotence, String numeroFacture, byte[] facturX);
}
