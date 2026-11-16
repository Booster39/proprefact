package com.proprefact.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Objets du domaine prêts à l'emploi pour les tests unitaires, sans base de données. */
public final class Exemples {
    private Exemples() {}

    public static Client client() {
        return new Client(new FicheClient(
                "Cabinet Durand", "732829320", "FR44732829320", "12 rue de la Paix", "69002", "Lyon", null));
    }

    /** Entreprise assujettie à la TVA, avec coordonnées bancaires et option pour les débits. */
    public static Entreprise vendeur() {
        return new Entreprise(new FicheEntreprise(
                "Net Plus SARL",
                "552100554",
                "FR00552100554",
                "5 rue des Lilas",
                "69003",
                "Lyon",
                "FR7630006000011234567890189",
                "AGRIFRPP",
                true));
    }

    public static LigneFacture ligne(String designation, String quantite, String prixUnitaireHt, String tauxTva) {
        return LigneFacture.ponctuelle(
                designation, new BigDecimal(quantite), new BigDecimal(prixUnitaireHt), new BigDecimal(tauxTva));
    }

    /** Une unité de « Prestation » au prix et au taux donnés. */
    public static LigneFacture ligne(String prixUnitaireHt, String tauxTva) {
        return ligne("Prestation", "1", prixUnitaireHt, tauxTva);
    }

    public static Facture brouillon(LigneFacture... lignes) {
        Facture facture = Facture.brouillon(client(), null);
        for (LigneFacture ligne : lignes) {
            facture.ajouter(ligne);
        }
        return facture;
    }

    public static Contrat contrat(LocalDate debut, LocalDate fin) {
        Site site = new Site(client(), "Bureaux Bellecour", "12 rue de la Paix", "69002", "Lyon");
        Contrat contrat =
                new Contrat(site, "Entretien des bureaux", new BigDecimal("480.00"), new BigDecimal("20"), debut);
        contrat.terminerLe(fin);
        return contrat;
    }
}
