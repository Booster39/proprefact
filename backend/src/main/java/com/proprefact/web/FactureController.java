package com.proprefact.web;

import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.proprefact.domain.Facture;
import com.proprefact.repo.FactureRepository;
import com.proprefact.service.FacturationService;
import com.proprefact.web.Dtos.FactureDto;
import com.proprefact.web.Dtos.FactureReq;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/factures")
class FactureController {
    private final FacturationService facturation;
    private final FactureRepository factures;

    FactureController(FacturationService facturation, FactureRepository factures) {
        this.facturation = facturation;
        this.factures = factures;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<FactureDto> lister() {
        return factures.findAllAvecClient().stream().map(FactureDto::resume).toList();
    }

    @GetMapping("/{id}")
    FactureDto detail(@PathVariable Long id) {
        return FactureDto.complete(facturation.charger(id));
    }

    @PostMapping
    FactureDto creer(@Valid @RequestBody FactureReq req) {
        return FactureDto.complete(facturation.creer(req));
    }

    /** Génère les brouillons du mois (paramètre mois=AAAA-MM) à partir des contrats. */
    @PostMapping("/generer-mensuelles")
    List<FactureDto> genererMensuelles(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return facturation.genererMensuelles(mois).stream().map(FactureDto::complete).toList();
    }

    @PostMapping("/{id}/valider")
    FactureDto valider(@PathVariable Long id) {
        return FactureDto.complete(facturation.valider(id));
    }

    @PostMapping("/{id}/deposer")
    FactureDto deposer(@PathVariable Long id) {
        return FactureDto.complete(facturation.deposer(id));
    }

    @PostMapping("/{id}/payee")
    FactureDto payee(@PathVariable Long id) {
        return FactureDto.complete(facturation.marquerPayee(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void supprimer(@PathVariable Long id) {
        facturation.supprimerBrouillon(id);
    }

    @GetMapping("/{id}/facturx")
    ResponseEntity<byte[]> facturX(@PathVariable Long id) {
        Facture f = facturation.charger(id);
        byte[] pdf = facturation.facturX(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.getNumero() + ".pdf").build().toString())
                .body(pdf);
    }
}
