package com.proprefact.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

/** L'entreprise de propreté abonnée au service (émettrice des factures). */
@Entity
@Getter
public class Entreprise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String raisonSociale;
    private String siren;
    private String siret;
    private String tvaIntra;
    private String adresse;
    private String codePostal;
    private String ville;
    private String iban;
    private String bic;
    /** Option pour le paiement de la TVA d'après les débits (mention obligatoire si choisie). */
    private boolean optionTvaDebits;

    /** Réservé à JPA. */
    protected Entreprise() {}

    public Entreprise(FicheEntreprise fiche) {
        mettreAJour(fiche);
    }

    /** Sans effet sur les factures déjà validées : elles gardent leur propre copie des coordonnées. */
    public void mettreAJour(FicheEntreprise fiche) {
        this.raisonSociale = fiche.raisonSociale();
        this.siren = fiche.siren();
        this.tvaIntra = fiche.tvaIntra();
        this.adresse = fiche.adresse();
        this.codePostal = fiche.codePostal();
        this.ville = fiche.ville();
        this.iban = fiche.iban();
        this.bic = fiche.bic();
        this.optionTvaDebits = fiche.optionTvaDebits();
    }
}
