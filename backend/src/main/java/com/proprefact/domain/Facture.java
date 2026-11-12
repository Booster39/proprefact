package com.proprefact.domain;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
@Getter
@Setter
public class Facture extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;
    /** Attribué à la validation, sans trou dans la séquence. */
    private String numero;
    @Enumerated(EnumType.STRING)
    private StatutFacture statut = StatutFacture.BROUILLON;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    /** Mois facturé au format AAAA-MM pour les factures issues des contrats. */
    private String periode;
    private BigDecimal totalHt = BigDecimal.ZERO;
    private BigDecimal totalTva = BigDecimal.ZERO;
    private BigDecimal totalTtc = BigDecimal.ZERO;
    /** Identifiant renvoyé par la plateforme agréée lors du dépôt. */
    private String idDepot;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LigneFacture> lignes = new ArrayList<>();

    public void ajouter(LigneFacture ligne) {
        ligne.setFacture(this);
        lignes.add(ligne);
        recalculerTotaux();
    }

    /** Base HT par taux de TVA ; la TVA est arrondie par taux, comme l'exige la norme EN 16931. */
    public Map<BigDecimal, BigDecimal> basesParTaux() {
        Map<BigDecimal, BigDecimal> bases = new TreeMap<>();
        for (LigneFacture l : lignes) {
            bases.merge(l.getTauxTva().stripTrailingZeros(), l.montantHt(), BigDecimal::add);
        }
        return bases;
    }

    public void recalculerTotaux() {
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal tva = BigDecimal.ZERO;
        for (var e : basesParTaux().entrySet()) {
            ht = ht.add(e.getValue());
            tva = tva.add(e.getValue().multiply(e.getKey()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        totalHt = ht;
        totalTva = tva;
        totalTtc = ht.add(tva);
    }
}
