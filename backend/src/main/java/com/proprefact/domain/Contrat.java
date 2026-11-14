package com.proprefact.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.Getter;
import lombok.Setter;

/** Prestation récurrente facturée chaque mois pour un site. */
@Entity
@Getter
@Setter
public class Contrat extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Site site;

    private String libelle;
    private BigDecimal montantMensuelHt;
    private BigDecimal tauxTva;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public boolean actifPendant(YearMonth mois) {
        return !dateDebut.isAfter(mois.atEndOfMonth()) && (dateFin == null || !dateFin.isBefore(mois.atDay(1)));
    }
}
