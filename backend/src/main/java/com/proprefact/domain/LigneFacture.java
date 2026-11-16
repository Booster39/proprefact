package com.proprefact.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import lombok.Getter;

/** Ligne d'une facture. Elle ne change plus une fois créée ; elle rejoint sa facture par {@link Facture#ajouter}. */
@Entity
@Getter
public class LigneFacture extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Facture facture;
    /** Renseigné pour une ligne issue d'un contrat ; avec la période, empêche de facturer deux fois le même mois. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Contrat contrat;

    private YearMonth periode;
    private String designation;
    private BigDecimal quantite;
    private BigDecimal prixUnitaireHt;
    private BigDecimal tauxTva;

    /** Réservé à JPA. */
    protected LigneFacture() {}

    private LigneFacture(String designation, BigDecimal quantite, BigDecimal prixUnitaireHt, BigDecimal tauxTva) {
        this.designation = designation;
        this.quantite = quantite;
        this.prixUnitaireHt = prixUnitaireHt;
        this.tauxTva = tauxTva;
    }

    /** Ligne saisie à la main (remise en état, vitrerie, intervention exceptionnelle...). */
    public static LigneFacture ponctuelle(
            String designation, BigDecimal quantite, BigDecimal prixUnitaireHt, BigDecimal tauxTva) {
        return new LigneFacture(designation, quantite, prixUnitaireHt, tauxTva);
    }

    /** Mensualité d'un contrat pour un mois : une unité au montant et au taux du contrat. */
    public static LigneFacture mensualite(Contrat contrat, YearMonth periode, String designation) {
        LigneFacture ligne =
                new LigneFacture(designation, BigDecimal.ONE, contrat.getMontantMensuelHt(), contrat.getTauxTva());
        ligne.contrat = contrat;
        ligne.periode = periode;
        return ligne;
    }

    /** Appelé par {@link Facture#ajouter} uniquement. */
    void rattacherA(Facture facture) {
        this.facture = facture;
    }

    public BigDecimal montantHt() {
        return quantite.multiply(prixUnitaireHt).setScale(2, RoundingMode.HALF_UP);
    }
}
