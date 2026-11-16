package com.proprefact.domain;

/** Coordonnées de l'entreprise abonnée ; {@code tvaIntra}, {@code iban} et {@code bic} peuvent être nuls. */
public record FicheEntreprise(
        String raisonSociale,
        String siren,
        String tvaIntra,
        String adresse,
        String codePostal,
        String ville,
        String iban,
        String bic,
        boolean optionTvaDebits) {}
