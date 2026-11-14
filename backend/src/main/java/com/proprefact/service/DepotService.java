package com.proprefact.service;

import com.proprefact.domain.Facture;
import com.proprefact.domain.StatutFacture;
import com.proprefact.tenant.TenantResolver;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dépôt des factures sur la plateforme agréée, en trois temps séparés :
 * <ol>
 * <li>l'intention de dépôt est enregistrée en base, avec une clé d'idempotence ;</li>
 * <li>l'appel à la plateforme est fait hors de toute transaction ;</li>
 * <li>le résultat est enregistré.</li>
 * </ol>
 * Si l'étape 2 ou 3 échoue (panne réseau, arrêt du serveur), la facture reste « dépôt en cours » et
 * l'appel est rejoué plus tard avec la même clé : la plateforme ne crée donc jamais deux dépôts.
 */
@Service
public class DepotService {
    private static final Logger log = LoggerFactory.getLogger(DepotService.class);

    /** Délai pendant lequel une tentative en cours n'est pas reprise par la relance automatique. */
    static final Duration BAIL = Duration.ofMinutes(5);

    static final Duration DELAI_INITIAL = Duration.ofMinutes(1);
    static final Duration DELAI_MAXIMAL = Duration.ofHours(6);

    private record Envoi(String cle, String numero, byte[] facturX) {}

    private final TransactionTemplate transaction;
    private final FacturationService facturation;
    private final PlateformeAgreee plateforme;
    private final TenantResolver tenant;
    private final EntityManager em;
    private final Clock horloge;

    DepotService(
            PlatformTransactionManager gestionnaire,
            FacturationService facturation,
            PlateformeAgreee plateforme,
            TenantResolver tenant,
            EntityManager em,
            Clock horloge) {
        this.transaction = new TransactionTemplate(gestionnaire);
        this.facturation = facturation;
        this.plateforme = plateforme;
        this.tenant = tenant;
        this.em = em;
        this.horloge = horloge;
    }

    /**
     * Dépose une facture validée, ou retente tout de suite un dépôt resté en cours.
     * Renvoie la facture : « déposée » si la plateforme a répondu, « dépôt en cours » sinon.
     */
    public Facture deposer(Long id) {
        Envoi envoi = transaction.execute(etat -> {
            Facture f = facturation.charger(id);
            // La clé n'est retenue que pour un premier dépôt : une nouvelle tentative garde la sienne.
            f.demanderDepot(UUID.randomUUID().toString(), horloge.instant().plus(BAIL));
            return new Envoi(
                    f.getCleDepot(), f.getNumero(), facturation.facturX(id).contenu());
        });

        String idDepot = null;
        String erreur = null;
        try {
            idDepot = plateforme.deposer(envoi.cle(), envoi.numero(), envoi.facturX());
        } catch (RuntimeException e) {
            log.warn("Dépôt de la facture {} en échec, il sera retenté", envoi.numero(), e);
            erreur = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
        return enregistrerResultat(id, idDepot, erreur);
    }

    private Facture enregistrerResultat(Long id, String idDepot, String erreur) {
        return transaction.execute(etat -> {
            Facture f = facturation.charger(id);
            if (f.getStatut() != StatutFacture.DEPOT_EN_COURS) {
                return f; // une autre tentative a déjà abouti
            }
            if (idDepot != null) {
                f.enregistrerDepotReussi(idDepot);
            } else {
                // + 1 : le délai se calcule sur le nombre d'échecs en comptant celui-ci.
                f.enregistrerDepotEchoue(erreur, horloge.instant().plus(delaiApres(f.getDepotTentatives() + 1)));
            }
            return f;
        });
    }

    /** Attente après le n-ième échec : 1 min, 2 min, 4 min... plafonnée à 6 heures. */
    static Duration delaiApres(int echecs) {
        long facteur = 1L << Math.min(Math.max(echecs - 1, 0), 20);
        Duration delai = DELAI_INITIAL.multipliedBy(facteur);
        return delai.compareTo(DELAI_MAXIMAL) > 0 ? DELAI_MAXIMAL : delai;
    }

    /**
     * Retente les dépôts en cours dont l'échéance est passée, toutes entreprises confondues.
     * Renvoie le nombre de dépôts retentés.
     */
    public int relancer(Instant maintenant) {
        // Requête SQL directe, volontairement hors du filtre par entreprise : elle ne lit que des identifiants.
        @SuppressWarnings("unchecked")
        List<Object[]> enAttente = transaction.execute(etat ->
                em.createNativeQuery("""
                select id, entreprise_id from facture
                where statut = 'DEPOT_EN_COURS' and depot_prochaine_tentative <= :maintenant
                order by depot_prochaine_tentative""").setParameter("maintenant", maintenant).getResultList());
        for (Object[] ligne : enAttente) {
            long id = ((Number) ligne[0]).longValue();
            long entreprise = ((Number) ligne[1]).longValue();
            try {
                tenant.executerPour(entreprise, () -> deposer(id));
            } catch (RuntimeException e) {
                log.error("Relance du dépôt de la facture {} impossible", id, e);
            }
        }
        return enAttente.size();
    }
}
