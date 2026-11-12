package com.proprefact.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.proprefact.config.TenantResolver;
import com.proprefact.domain.Entreprise;
import com.proprefact.domain.Utilisateur;
import com.proprefact.repo.EntrepriseRepository;
import com.proprefact.repo.UtilisateurRepository;
import com.proprefact.web.Dtos.ConnexionReq;
import com.proprefact.web.Dtos.InscriptionReq;
import com.proprefact.web.Dtos.JetonDto;

@Service
public class AuthService {
    private final EntrepriseRepository entreprises;
    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encodeur;
    private final JwtEncoder jwt;
    private final Duration duree;

    AuthService(EntrepriseRepository entreprises, UtilisateurRepository utilisateurs, PasswordEncoder encodeur,
            JwtEncoder jwt, @Value("${app.jwt.duree-heures}") long dureeHeures) {
        this.entreprises = entreprises;
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
        this.jwt = jwt;
        this.duree = Duration.ofHours(dureeHeures);
    }

    @Transactional
    public JetonDto inscrire(InscriptionReq req) {
        if (utilisateurs.findByEmailIgnoreCase(req.email()).isPresent() || entreprises.existsBySiren(req.siren())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà pour cet e-mail ou ce SIREN");
        }
        Entreprise e = new Entreprise();
        e.setRaisonSociale(req.raisonSociale());
        e.setSiren(req.siren());
        e.setTvaIntra(vide(req.tvaIntra()));
        e.setAdresse(req.adresse());
        e.setCodePostal(req.codePostal());
        e.setVille(req.ville());
        e.setIban(vide(req.iban()));
        e.setBic(vide(req.bic()));
        e.setOptionTvaDebits(Boolean.TRUE.equals(req.optionTvaDebits()));
        entreprises.save(e);

        Utilisateur u = new Utilisateur();
        u.setEmail(req.email().toLowerCase());
        u.setMotDePasseHash(encodeur.encode(req.motDePasse()));
        u.setEntreprise(e);
        utilisateurs.save(u);
        return jeton(u);
    }

    @Transactional(readOnly = true)
    public JetonDto connecter(ConnexionReq req) {
        Utilisateur u = utilisateurs.findByEmailIgnoreCase(req.email())
                .filter(x -> encodeur.matches(req.motDePasse(), x.getMotDePasseHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects"));
        return jeton(u);
    }

    private JetonDto jeton(Utilisateur u) {
        Instant maintenant = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(u.getEmail())
                .issuedAt(maintenant)
                .expiresAt(maintenant.plus(duree))
                .claim(TenantResolver.CLAIM_ENTREPRISE, u.getEntreprise().getId())
                .build();
        String valeur = jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new JetonDto(valeur, u.getEntreprise().getRaisonSociale());
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
