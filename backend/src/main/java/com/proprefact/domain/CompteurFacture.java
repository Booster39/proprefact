package com.proprefact.domain;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/** Dernier numéro de facture attribué pour une année. */
@Entity
@Getter
@Setter
public class CompteurFacture extends TenantEntity {
    private int annee;
    private int dernierNumero;
}
