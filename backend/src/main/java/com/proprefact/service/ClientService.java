package com.proprefact.service;

import com.proprefact.domain.Client;
import com.proprefact.domain.FicheClient;
import com.proprefact.domain.IntrouvableException;
import com.proprefact.domain.Site;
import com.proprefact.repo.ClientRepository;
import com.proprefact.repo.SiteRepository;
import com.proprefact.service.commande.ClientCommande;
import com.proprefact.service.commande.SiteCommande;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Clients de l'entreprise et leurs sites d'intervention. */
@Service
@Transactional
public class ClientService {
    private final ClientRepository clients;
    private final SiteRepository sites;

    ClientService(ClientRepository clients, SiteRepository sites) {
        this.clients = clients;
        this.sites = sites;
    }

    @Transactional(readOnly = true)
    public List<Client> lister() {
        return clients.findAllByOrderByRaisonSociale();
    }

    public Client creer(ClientCommande commande) {
        return clients.save(new Client(fiche(commande)));
    }

    /** Sans effet sur les factures déjà validées : elles gardent leur propre copie des coordonnées. */
    public Client modifier(Long id, ClientCommande commande) {
        Client client = trouver(id);
        client.mettreAJour(fiche(commande));
        return client;
    }

    @Transactional(readOnly = true)
    public List<Site> sites(Long clientId) {
        return sites.findByClientIdOrderByNom(trouver(clientId).getId());
    }

    public Site ajouterSite(Long clientId, SiteCommande commande) {
        return sites.save(new Site(
                trouver(clientId), commande.nom(), commande.adresse(), commande.codePostal(), commande.ville()));
    }

    private Client trouver(Long id) {
        return clients.findById(id).orElseThrow(() -> new IntrouvableException("Client introuvable"));
    }

    private static FicheClient fiche(ClientCommande commande) {
        return new FicheClient(
                commande.raisonSociale(),
                commande.siren(),
                Textes.ouNull(commande.tvaIntra()),
                commande.adresse(),
                commande.codePostal(),
                commande.ville(),
                Textes.ouNull(commande.email()));
    }
}
