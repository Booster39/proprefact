package com.proprefact.web;

import com.proprefact.service.ClientService;
import com.proprefact.service.commande.ClientCommande;
import com.proprefact.service.commande.SiteCommande;
import com.proprefact.web.Dtos.ClientDto;
import com.proprefact.web.Dtos.SiteDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
class ClientController {
    private final ClientService clients;

    ClientController(ClientService clients) {
        this.clients = clients;
    }

    @GetMapping
    List<ClientDto> lister() {
        return clients.lister().stream().map(ClientDto::de).toList();
    }

    @PostMapping
    ClientDto creer(@Valid @RequestBody ClientCommande commande) {
        return ClientDto.de(clients.creer(commande));
    }

    @PutMapping("/{id}")
    ClientDto modifier(@PathVariable Long id, @Valid @RequestBody ClientCommande commande) {
        return ClientDto.de(clients.modifier(id, commande));
    }

    @GetMapping("/{id}/sites")
    List<SiteDto> sites(@PathVariable Long id) {
        return clients.sites(id).stream().map(SiteDto::de).toList();
    }

    @PostMapping("/{id}/sites")
    SiteDto ajouterSite(@PathVariable Long id, @Valid @RequestBody SiteCommande commande) {
        return SiteDto.de(clients.ajouterSite(id, commande));
    }
}
