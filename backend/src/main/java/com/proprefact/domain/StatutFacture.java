package com.proprefact.domain;

public enum StatutFacture {
    /** Modifiable, sans numéro. */
    BROUILLON,
    /** Numérotée et figée. */
    VALIDEE,
    /** Dépôt demandé : l'envoi à la plateforme agréée est en cours ou sera retenté. */
    DEPOT_EN_COURS,
    /** Transmise à la plateforme agréée. */
    DEPOSEE,
    PAYEE
}
