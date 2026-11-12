package com.proprefact.domain;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
@Getter
@Setter
public class LigneFacture extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Facture facture;
    /** Renseigné pour une ligne issue d'un contrat ; avec la période, empêche de facturer deux fois le même mois. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Contrat contrat;
    private String periode;
    private String designation;
    private BigDecimal quantite;
    private BigDecimal prixUnitaireHt;
    private BigDecimal tauxTva;

    public BigDecimal montantHt() {
        return quantite.multiply(prixUnitaireHt).setScale(2, RoundingMode.HALF_UP);
    }
}
