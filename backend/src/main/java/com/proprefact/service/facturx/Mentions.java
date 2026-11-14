package com.proprefact.service.facturx;

/** Mentions légales reprises à l'identique dans le PDF et dans le XML. Leur exactitude relève de la conformité. */
final class Mentions {
    private Mentions() {}

    static final String PENALITES = "En cas de retard de paiement, pénalités au taux de trois fois le taux "
            + "d'intérêt légal, exigibles sans rappel.";
    static final String INDEMNITE =
            "Indemnité forfaitaire pour frais de recouvrement en cas de retard de paiement : 40 €.";
    static final String ESCOMPTE = "Pas d'escompte pour paiement anticipé.";
    static final String FRANCHISE = "TVA non applicable, art. 293 B du CGI";
}
