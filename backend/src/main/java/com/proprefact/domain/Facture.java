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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import org.hibernate.annotations.EmbeddedColumnNaming;

@Entity
@Getter
public class Facture extends TenantEntity {
    /** Taille de la colonne facture.depot_erreur. */
    static final int LONGUEUR_MAX_ERREUR = 500;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;
    /** Attribué à la validation, sans trou dans la séquence. */
    private String numero;

    @Enumerated(EnumType.STRING)
    private StatutFacture statut = StatutFacture.BROUILLON;

    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    /** Mois facturé, pour les factures issues des contrats ; nul pour une facture ponctuelle. */
    private YearMonth periode;

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

    /** Réservé à JPA : une facture se crée avec {@link #brouillon}. */
    protected Facture() {}

    /** Nouveau brouillon ; la période est nulle pour une facture ponctuelle. */
    public static Facture brouillon(Client client, YearMonth periode) {
        Facture facture = new Facture();
        facture.client = client;
        facture.periode = periode;
        return facture;
    }

    /** Attribue le numéro définitif et fige la facture : elle ne dépend plus des fiches client et entreprise. */
    public void valider(String numero, LocalDate jour, int delaiPaiementJours, Entreprise vendeur) {
        exiger(StatutFacture.BROUILLON);
        this.numero = numero;
        this.dateEmission = jour;
        this.dateEcheance = jour.plusDays(delaiPaiementJours);
        recalculerTotaux();
        figerParties(vendeur);
        this.statut = StatutFacture.VALIDEE;
    }

    /** Enregistre l'intention de dépôt, ou prolonge le bail d'un dépôt déjà en cours. */
    public void demanderDepot(String nouvelleCle, Instant finDuBail) {
        if (statut == StatutFacture.VALIDEE) {
            statut = StatutFacture.DEPOT_EN_COURS;
            cleDepot = nouvelleCle;
            depotTentatives = 0;
        } else if (statut != StatutFacture.DEPOT_EN_COURS) {
            throw new TransitionInterditeException("Action impossible : la facture est au statut " + statut);
        }
        depotProchaineTentative = finDuBail;
    }

    public void enregistrerDepotReussi(String idDepot) {
        this.idDepot = idDepot;
        this.statut = StatutFacture.DEPOSEE;
        this.depotErreur = null;
        this.depotProchaineTentative = null;
    }

    public void enregistrerDepotEchoue(String erreur, Instant prochaineTentative) {
        this.depotTentatives++;
        this.depotErreur = erreur.length() > LONGUEUR_MAX_ERREUR ? erreur.substring(0, LONGUEUR_MAX_ERREUR) : erreur;
        this.depotProchaineTentative = prochaineTentative;
    }

    public void marquerPayee() {
        if (statut != StatutFacture.VALIDEE && statut != StatutFacture.DEPOSEE) {
            throw new TransitionInterditeException("Seule une facture validée peut être marquée payée");
        }
        statut = StatutFacture.PAYEE;
    }

    public void exiger(StatutFacture attendu) {
        if (statut != attendu) {
            throw new TransitionInterditeException("Action impossible : la facture est au statut " + statut);
        }
    }

    /** Recopie les coordonnées actuelles du vendeur et de l'acheteur : la facture n'en dépend plus ensuite. */
    private void figerParties(Entreprise entreprise) {
        vendeur = PartieFacture.de(entreprise);
        vendeurIban = entreprise.getIban();
        vendeurBic = entreprise.getBic();
        vendeurOptionTvaDebits = entreprise.isOptionTvaDebits();
        acheteur = PartieFacture.de(client);
    }

    public void ajouter(LigneFacture ligne) {
        ligne.rattacherA(this);
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

    /** TVA par taux, arrondie par taux. Source unique pour les totaux et pour le PDF. */
    public Map<BigDecimal, BigDecimal> tvaParTaux() {
        Map<BigDecimal, BigDecimal> tva = new TreeMap<>();
        basesParTaux()
                .forEach((taux, base) ->
                        tva.put(taux, base.multiply(taux).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));
        return tva;
    }

    private void recalculerTotaux() {
        totalHt = basesParTaux().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        totalTva = tvaParTaux().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        totalTtc = totalHt.add(totalTva);
    }
}
