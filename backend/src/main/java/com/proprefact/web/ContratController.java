package com.proprefact.web;

import com.proprefact.service.ContratService;
import com.proprefact.service.commande.ContratCommande;
import com.proprefact.web.Dtos.ContratDto;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contrats")
class ContratController {
    private final ContratService contrats;

    ContratController(ContratService contrats) {
        this.contrats = contrats;
    }

    @GetMapping
    List<ContratDto> lister() {
        return contrats.lister().stream().map(ContratDto::de).toList();
    }

    @PostMapping
    ContratDto creer(@Valid @RequestBody ContratCommande commande) {
        return ContratDto.de(contrats.creer(commande));
    }

    @PostMapping("/{id}/resilier")
    ContratDto resilier(@PathVariable Long id, @RequestParam LocalDate dateFin) {
        return ContratDto.de(contrats.resilier(id, dateFin));
    }
}
