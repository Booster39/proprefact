package com.proprefact.service;

/**
 * Plateforme agréée (PA) par laquelle les factures électroniques doivent transiter.
 * Toute l'intégration avec le partenaire choisi passe par cette interface, pour pouvoir en changer.
 */
public interface PlateformeAgreee {

    /** Dépose une facture Factur-X et renvoie l'identifiant de dépôt attribué par la plateforme. */
    String deposer(String numeroFacture, byte[] facturX);
}
