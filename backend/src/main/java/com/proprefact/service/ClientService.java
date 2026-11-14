package com.proprefact.service;

import com.proprefact.domain.Client;
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
        return clients.save(remplir(new Client(), commande));
    }

    /** Sans effet sur les factures déjà validées : elles gardent leur propre copie des coordonnées. */
    public Client modifier(Long id, ClientCommande commande) {
        return remplir(trouver(id), commande);
    }

    @Transactional(readOnly = true)
    public List<Site> sites(Long clientId) {
        return sites.findByClientIdOrderByNom(trouver(clientId).getId());
    }

    public Site ajouterSite(Long clientId, SiteCommande commande) {
        Site site = new Site();
        site.setClient(trouver(clientId));
        site.setNom(commande.nom());
        site.setAdresse(commande.adresse());
        site.setCodePostal(commande.codePostal());
        site.setVille(commande.ville());
        return sites.save(site);
    }

    private Client trouver(Long id) {
        return clients.findById(id).orElseThrow(() -> new IntrouvableException("Client introuvable"));
    }

    private static Client remplir(Client client, ClientCommande commande) {
        client.setRaisonSociale(commande.raisonSociale());
        client.setSiren(commande.siren());
        client.setTvaIntra(Textes.ouNull(commande.tvaIntra()));
        client.setAdresse(commande.adresse());
        client.setCodePostal(commande.codePostal());
        client.setVille(commande.ville());
        client.setEmail(Textes.ouNull(commande.email()));
        return client;
    }
}
