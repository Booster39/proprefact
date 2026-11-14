package com.proprefact.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.EmbeddedColumnNaming;

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

    /** Empêche deux actions simultanées (double clic, relance automatique) de s'écraser. */
    @Version
    private long version;
    /** Clé d'idempotence transmise à la plateforme : identique à chaque nouvelle tentative. */
    private String cleDepot;

    private int depotTentatives;
    private Instant depotProchaineTentative;
    /** Message de la dernière tentative en échec, affiché à l'utilisateur. */
    private String depotErreur;

    /** Copies figées à la validation ; vides tant que la facture est un brouillon. */
    @Embedded
    @EmbeddedColumnNaming("vendeur_%s")
    private PartieFacture vendeur;

    @Embedded
    @EmbeddedColumnNaming("acheteur_%s")
    private PartieFacture acheteur;

    private String vendeurIban;
    private String vendeurBic;
    private Boolean vendeurOptionTvaDebits;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LigneFacture> lignes = new ArrayList<>();

    /** Recopie les coordonnées actuelles du vendeur et de l'acheteur : la facture n'en dépend plus ensuite. */
    public void figerParties(Entreprise entreprise) {
        vendeur = PartieFacture.de(entreprise);
        vendeurIban = entreprise.getIban();
        vendeurBic = entreprise.getBic();
        vendeurOptionTvaDebits = entreprise.isOptionTvaDebits();
        acheteur = PartieFacture.de(client);
    }

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
