package com.proprefact.domain;

/** Coordonnées d'un client telles qu'elles sont saisies ; {@code tvaIntra} et {@code email} peuvent être nuls. */
public record FicheClient(
        String raisonSociale,
        String siren,
        String tvaIntra,
        String adresse,
        String codePostal,
        String ville,
        String email) {}
