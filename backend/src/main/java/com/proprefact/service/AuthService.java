package com.proprefact.service;

import com.proprefact.domain.AdresseNonConfirmeeException;
import com.proprefact.domain.Entreprise;
import com.proprefact.domain.IdentifiantsIncorrectsException;
import com.proprefact.domain.Utilisateur;
import com.proprefact.domain.ValeurRefuseeException;
import com.proprefact.proprietes.ProprietesApp;
import com.proprefact.repo.EntrepriseRepository;
import com.proprefact.repo.UtilisateurRepository;
import com.proprefact.service.commande.ConnexionCommande;
import com.proprefact.service.commande.InscriptionCommande;
import com.proprefact.service.commande.JetonConnexion;
import com.proprefact.tenant.TenantResolver;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    static final Duration VALIDITE_CONFIRMATION = Duration.ofHours(24);

    private final EntrepriseRepository entreprises;
    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encodeur;
    private final JwtEncoder jwt;
    private final ApplicationEventPublisher evenements;
    private final Clock horloge;
    private final Duration duree;
    private final String urlPublique;
    private final SecureRandom alea = new SecureRandom();
    /** Sert à dépenser le même temps de calcul quand l'e-mail est inconnu que quand il existe. */
    private final String hashFactice;

    AuthService(
            EntrepriseRepository entreprises,
            UtilisateurRepository utilisateurs,
            PasswordEncoder encodeur,
            JwtEncoder jwt,
            ApplicationEventPublisher evenements,
            ProprietesApp proprietes,
            Clock horloge) {
        this.entreprises = entreprises;
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
        this.jwt = jwt;
        this.evenements = evenements;
        this.horloge = horloge;
        this.duree = Duration.ofHours(proprietes.jwt().dureeHeures());
        this.urlPublique = proprietes.urlPublique().replaceAll("/+$", "");
        this.hashFactice = encodeur.encode(jetonAleatoire());
    }

    /**
     * Crée le compte et envoie le lien de confirmation. Le déroulement visible est le même que l'e-mail
     * soit nouveau ou déjà inscrit : l'appelant ne peut pas en déduire quels comptes existent.
     */
    @Transactional
    public void inscrire(InscriptionCommande req) {
        String email = Textes.emailNormalise(req.email());
        String hash = encodeur.encode(req.motDePasse());
        Optional<Utilisateur> existant = utilisateurs.findByEmailIgnoreCase(email);
        if (existant.isPresent() && existant.get().isEmailVerifie()) {
            evenements.publishEvent(
                    new EmailAEnvoyer(email, "Vous avez déjà un compte ProPreFact", """
                    Bonjour,

                    Une inscription vient d'être demandée avec cette adresse, mais un compte existe déjà.
                    Connectez-vous ici : %s/connexion

                    Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.
                    """.formatted(urlPublique)));
            return;
        }
        // Une inscription jamais confirmée est remplacée : elle ne bloque pas l'adresse.
        Utilisateur u = existant.orElseGet(Utilisateur::new);
        Entreprise e = u.getEntreprise() != null ? u.getEntreprise() : new Entreprise();
        e.setRaisonSociale(req.raisonSociale());
        e.setSiren(req.siren());
        e.setTvaIntra(Textes.ouNull(req.tvaIntra()));
        e.setAdresse(req.adresse());
        e.setCodePostal(req.codePostal());
        e.setVille(req.ville());
        e.setIban(Textes.ouNull(req.iban()));
        e.setBic(Textes.ouNull(req.bic()));
        e.setOptionTvaDebits(Boolean.TRUE.equals(req.optionTvaDebits()));
        entreprises.save(e);

        u.setEmail(email);
        u.setMotDePasseHash(hash);
        u.setEntreprise(e);
        utilisateurs.save(u);
        envoyerConfirmation(u);
    }

    /** Renvoie le lien si le compte attend encore sa confirmation ; ne révèle rien sinon. */
    @Transactional
    public void renvoyerConfirmation(String email) {
        utilisateurs
                .findByEmailIgnoreCase(Textes.emailNormalise(email))
                .filter(u -> !u.isEmailVerifie())
                .ifPresent(this::envoyerConfirmation);
    }

    @Transactional
    public JetonConnexion confirmer(String jetonConfirmation) {
        Utilisateur u = utilisateurs
                .findByJetonConfirmationHash(Empreinte.sha256(jetonConfirmation))
                .filter(x ->
                        x.getJetonExpireLe() != null && x.getJetonExpireLe().isAfter(horloge.instant()))
                .orElseThrow(() -> new ValeurRefuseeException("Lien de confirmation invalide ou expiré"));
        u.setEmailVerifie(true);
        u.setJetonConfirmationHash(null);
        u.setJetonExpireLe(null);
        return jeton(u);
    }

    @Transactional(readOnly = true)
    public JetonConnexion connecter(ConnexionCommande req) {
        Optional<Utilisateur> trouve = utilisateurs.findByEmailIgnoreCase(Textes.emailNormalise(req.email()));
        boolean motDePasseCorrect = encodeur.matches(
                req.motDePasse(), trouve.map(Utilisateur::getMotDePasseHash).orElse(hashFactice));
        if (trouve.isEmpty() || !motDePasseCorrect) {
            throw new IdentifiantsIncorrectsException();
        }
        if (!trouve.get().isEmailVerifie()) {
            throw new AdresseNonConfirmeeException();
        }
        return jeton(trouve.get());
    }

    private void envoyerConfirmation(Utilisateur u) {
        String jetonConfirmation = jetonAleatoire();
        u.setJetonConfirmationHash(Empreinte.sha256(jetonConfirmation));
        u.setJetonExpireLe(horloge.instant().plus(VALIDITE_CONFIRMATION));
        evenements.publishEvent(
                new EmailAEnvoyer(u.getEmail(), "Confirmez votre adresse e-mail – ProPreFact", """
                Bonjour,

                Pour activer votre compte ProPreFact, ouvrez ce lien (valable 24 heures) :
                %s/confirmation?jeton=%s

                Si vous n'êtes pas à l'origine de cette inscription, ignorez ce message.
                """.formatted(
                                urlPublique, jetonConfirmation)));
    }

    private JetonConnexion jeton(Utilisateur u) {
        Instant maintenant = horloge.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(u.getEmail())
                .issuedAt(maintenant)
                .expiresAt(maintenant.plus(duree))
                .claim(TenantResolver.CLAIM_ENTREPRISE, u.getEntreprise().getId())
                .build();
        String valeur = jwt.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new JetonConnexion(valeur, u.getEntreprise().getRaisonSociale());
    }

    private String jetonAleatoire() {
        byte[] octets = new byte[32];
        alea.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }
}
