package com.proprefact.domain;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** L'entreprise de propreté abonnée au service (émettrice des factures). */
@Entity
@Getter
@Setter
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
}
