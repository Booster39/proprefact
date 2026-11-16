package com.proprefact.domain;

import jakarta.persistence.Entity;
import lombok.Getter;

/** Dernier numéro de facture attribué pour une année. Créé par la base (CompteurFactureRepository.creerSiAbsent). */
@Entity
@Getter
public class CompteurFacture extends TenantEntity {
    private int annee;
    private int dernierNumero;

    /** Réservé à JPA. */
    protected CompteurFacture() {}

    /** Attribue le numéro suivant, sans trou. À appeler sous le verrou de CompteurFactureRepository.findByAnnee. */
    public int suivant() {
        return ++dernierNumero;
    }
}
