package com.proprefact.domain;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Entity;

/** Dernier numéro de facture attribué pour une année. */
@Entity
@Getter
@Setter
public class CompteurFacture extends TenantEntity {
    private int annee;
    private int dernierNumero;
}
