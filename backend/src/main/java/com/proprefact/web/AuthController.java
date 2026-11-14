package com.proprefact.web;

import com.proprefact.service.AuthService;
import com.proprefact.service.LimiteurTentatives;
import com.proprefact.service.commande.ConnexionCommande;
import com.proprefact.service.commande.InscriptionCommande;
import com.proprefact.service.commande.JetonConnexion;
import com.proprefact.web.Dtos.ConfirmationReq;
import com.proprefact.web.Dtos.EmailReq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    static final Duration FENETRE = Duration.ofMinutes(15);

    private final AuthService auth;
    private final LimiteurTentatives limiteur;
    private final int maxParIp;
    private final int maxEchecsParEmail;

    AuthController(
            AuthService auth,
            LimiteurTentatives limiteur,
            @Value("${app.auth.max-requetes-par-ip}") int maxParIp,
            @Value("${app.auth.max-echecs-par-email}") int maxEchecsParEmail) {
        this.auth = auth;
        this.limiteur = limiteur;
        this.maxParIp = maxParIp;
        this.maxEchecsParEmail = maxEchecsParEmail;
    }

    /** Répond toujours 202, que l'adresse soit nouvelle ou déjà inscrite. */
    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void inscription(@Valid @RequestBody InscriptionCommande req, HttpServletRequest http) {
        limiterParIp(http);
        auth.inscrire(req);
    }

    @PostMapping("/renvoi-confirmation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void renvoiConfirmation(@Valid @RequestBody EmailReq req, HttpServletRequest http) {
        limiterParIp(http);
        auth.renvoyerConfirmation(req.email());
    }

    @PostMapping("/confirmation")
    JetonConnexion confirmation(@Valid @RequestBody ConfirmationReq req, HttpServletRequest http) {
        limiterParIp(http);
        return auth.confirmer(req.jeton());
    }

    @PostMapping("/connexion")
    JetonConnexion connexion(@Valid @RequestBody ConnexionCommande req, HttpServletRequest http) {
        limiterParIp(http);
        String cleEchecs = "echecs:" + req.email().trim().toLowerCase();
        if (limiteur.atteinte(cleEchecs, maxEchecsParEmail, FENETRE)) {
            throw tropDeTentatives();
        }
        try {
            JetonConnexion jeton = auth.connecter(req);
            limiteur.oublier(cleEchecs);
            return jeton;
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                limiteur.tenter(cleEchecs, maxEchecsParEmail, FENETRE);
            }
            throw e;
        }
    }

    private void limiterParIp(HttpServletRequest http) {
        if (!limiteur.tenter("ip:" + http.getRemoteAddr(), maxParIp, FENETRE)) {
            throw tropDeTentatives();
        }
    }

    private static ResponseStatusException tropDeTentatives() {
        return new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives : réessayez dans quelques minutes");
    }
}
