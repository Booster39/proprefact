package com.proprefact.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proprefact.service.AuthService;
import com.proprefact.web.Dtos.ConnexionReq;
import com.proprefact.web.Dtos.InscriptionReq;
import com.proprefact.web.Dtos.JetonDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/inscription")
    JetonDto inscription(@Valid @RequestBody InscriptionReq req) {
        return auth.inscrire(req);
    }

    @PostMapping("/connexion")
    JetonDto connexion(@Valid @RequestBody ConnexionReq req) {
        return auth.connecter(req);
    }
}
