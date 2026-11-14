package com.proprefact.web;

import com.proprefact.domain.Client;
import com.proprefact.domain.Site;
import com.proprefact.repo.ClientRepository;
import com.proprefact.repo.SiteRepository;
import com.proprefact.web.Dtos.ClientDto;
import com.proprefact.web.Dtos.ClientReq;
import com.proprefact.web.Dtos.SiteDto;
import com.proprefact.web.Dtos.SiteReq;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clients")
@Transactional
class ClientController {
    private final ClientRepository clients;
    private final SiteRepository sites;

    ClientController(ClientRepository clients, SiteRepository sites) {
        this.clients = clients;
        this.sites = sites;
    }

    @GetMapping
    List<ClientDto> lister() {
        return clients.findAllByOrderByRaisonSociale().stream()
                .map(ClientDto::de)
                .toList();
    }

    @PostMapping
    ClientDto creer(@Valid @RequestBody ClientReq req) {
        return ClientDto.de(clients.save(remplir(new Client(), req)));
    }

    @PutMapping("/{id}")
    ClientDto modifier(@PathVariable Long id, @Valid @RequestBody ClientReq req) {
        return ClientDto.de(remplir(trouver(id), req));
    }

    @GetMapping("/{id}/sites")
    List<SiteDto> sites(@PathVariable Long id) {
        return sites.findByClientIdOrderByNom(trouver(id).getId()).stream()
                .map(SiteDto::de)
                .toList();
    }

    @PostMapping("/{id}/sites")
    SiteDto ajouterSite(@PathVariable Long id, @Valid @RequestBody SiteReq req) {
        Site s = new Site();
        s.setClient(trouver(id));
        s.setNom(req.nom());
        s.setAdresse(req.adresse());
        s.setCodePostal(req.codePostal());
        s.setVille(req.ville());
        return SiteDto.de(sites.save(s));
    }

    private Client trouver(Long id) {
        return clients.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));
    }

    private static Client remplir(Client c, ClientReq req) {
        c.setRaisonSociale(req.raisonSociale());
        c.setSiren(req.siren());
        c.setTvaIntra(
                req.tvaIntra() == null || req.tvaIntra().isBlank()
                        ? null
                        : req.tvaIntra().trim());
        c.setAdresse(req.adresse());
        c.setCodePostal(req.codePostal());
        c.setVille(req.ville());
        c.setEmail(
                req.email() == null || req.email().isBlank()
                        ? null
                        : req.email().trim());
        return c;
    }
}
