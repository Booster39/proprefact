package com.proprefact.service;

import com.proprefact.domain.Contrat;
import com.proprefact.domain.IntrouvableException;
import com.proprefact.domain.Site;
import com.proprefact.repo.ContratRepository;
import com.proprefact.repo.SiteRepository;
import com.proprefact.service.commande.ContratCommande;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contrats récurrents, facturés chaque mois par {@link FacturationService}. */
@Service
@Transactional
public class ContratService {
    private final ContratRepository contrats;
    private final SiteRepository sites;

    ContratService(ContratRepository contrats, SiteRepository sites) {
        this.contrats = contrats;
        this.sites = sites;
    }

    /** Les contrats sont renvoyés avec leur site et leur client déjà chargés. */
    @Transactional(readOnly = true)
    public List<Contrat> lister() {
        return contrats.findAllAvecSiteEtClient();
    }

    public Contrat creer(ContratCommande commande) {
        Site site = sites.findById(commande.siteId()).orElseThrow(() -> new IntrouvableException("Site introuvable"));
        Contrat contrat = new Contrat();
        contrat.setSite(site);
        contrat.setLibelle(commande.libelle());
        contrat.setMontantMensuelHt(commande.montantMensuelHt());
        contrat.setTauxTva(commande.tauxTva());
        contrat.setDateDebut(commande.dateDebut());
        contrat.terminerLe(commande.dateFin());
        return avecSiteEtClient(contrats.save(contrat));
    }

    /** Un contrat ne se supprime pas (il a pu être facturé) : on lui donne une date de fin. */
    public Contrat resilier(Long id, LocalDate dateFin) {
        Contrat contrat = contrats.findById(id).orElseThrow(() -> new IntrouvableException("Contrat introuvable"));
        contrat.terminerLe(dateFin);
        return avecSiteEtClient(contrat);
    }

    /** Charge le site et le client tant que la transaction est ouverte : l'affichage en a besoin. */
    private static Contrat avecSiteEtClient(Contrat contrat) {
        contrat.getSite().getClient().getRaisonSociale();
        return contrat;
    }
}
