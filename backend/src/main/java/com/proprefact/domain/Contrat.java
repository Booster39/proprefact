package com.proprefact.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.Getter;

/** Prestation récurrente facturée chaque mois pour un site. */
@Entity
@Getter
public class Contrat extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Site site;

    private String libelle;
    private BigDecimal montantMensuelHt;
    private BigDecimal tauxTva;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    /** Réservé à JPA. */
    protected Contrat() {}

    /** Contrat sans date de fin ; elle se fixe ensuite avec {@link #terminerLe}. */
    public Contrat(Site site, String libelle, BigDecimal montantMensuelHt, BigDecimal tauxTva, LocalDate dateDebut) {
        this.site = site;
        this.libelle = libelle;
        this.montantMensuelHt = montantMensuelHt;
        this.tauxTva = tauxTva;
        this.dateDebut = dateDebut;
    }

    /** Fixe ou retire (null) la date de fin, qui ne peut pas précéder le début. */
    public void terminerLe(LocalDate fin) {
        if (fin != null && fin.isBefore(dateDebut)) {
            throw new ValeurRefuseeException("La date de fin précède la date de début");
        }
        this.dateFin = fin;
    }

    public boolean actifPendant(YearMonth mois) {
        return !dateDebut.isAfter(mois.atEndOfMonth()) && (dateFin == null || !dateFin.isBefore(mois.atDay(1)));
    }
}
