package com.proprefact.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proprefact.domain.Contrat;
import com.proprefact.repo.ContratRepository;
import com.proprefact.repo.SiteRepository;
import com.proprefact.web.Dtos.ContratDto;
import com.proprefact.web.Dtos.ContratReq;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contrats")
@Transactional
class ContratController {
    private final ContratRepository contrats;
    private final SiteRepository sites;

    ContratController(ContratRepository contrats, SiteRepository sites) {
        this.contrats = contrats;
        this.sites = sites;
    }

    @GetMapping
    List<ContratDto> lister() {
        return contrats.findAllAvecSiteEtClient().stream().map(ContratDto::de).toList();
    }

    @PostMapping
    ContratDto creer(@Valid @RequestBody ContratReq req) {
        if (req.dateFin() != null && req.dateFin().isBefore(req.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin précède la date de début");
        }
        Contrat c = new Contrat();
        c.setSite(sites.findById(req.siteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site introuvable")));
        c.setLibelle(req.libelle());
        c.setMontantMensuelHt(req.montantMensuelHt());
        c.setTauxTva(req.tauxTva());
        c.setDateDebut(req.dateDebut());
        c.setDateFin(req.dateFin());
        return ContratDto.de(contrats.save(c));
    }

    /** Un contrat ne se supprime pas (il a pu être facturé) : on lui donne une date de fin. */
    @PostMapping("/{id}/resilier")
    ContratDto resilier(@PathVariable Long id, @RequestParam LocalDate dateFin) {
        Contrat c = contrats.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrat introuvable"));
        if (dateFin.isBefore(c.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin précède la date de début");
        }
        c.setDateFin(dateFin);
        return ContratDto.de(c);
    }
}
