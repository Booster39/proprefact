package com.proprefact.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.Getter;

@Entity
@Getter
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String motDePasseHash;
    /** Le compte ne permet de se connecter qu'une fois l'adresse confirmée. */
    private boolean emailVerifie;
    /** Empreinte SHA-256 du jeton envoyé par e-mail ; le jeton lui-même n'est jamais stocké. */
    private String jetonConfirmationHash;

    private Instant jetonExpireLe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Entreprise entreprise;

    /** Réservé à JPA. */
    protected Utilisateur() {}

    /** Nouveau compte, inutilisable tant que l'adresse n'est pas confirmée. */
    public Utilisateur(String email, String motDePasseHash, Entreprise entreprise) {
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.entreprise = entreprise;
    }

    /** Une inscription jamais confirmée est remplacée par la suivante : elle ne bloque pas l'adresse. */
    public void remplacerInscription(String motDePasseHash) {
        if (emailVerifie) {
            throw new TransitionInterditeException("Ce compte est déjà confirmé");
        }
        this.motDePasseHash = motDePasseHash;
    }

    /** Enregistre l'empreinte du lien envoyé ; un lien précédent cesse d'être valable. */
    public void attendreConfirmation(String jetonHash, Instant expireLe) {
        this.jetonConfirmationHash = jetonHash;
        this.jetonExpireLe = expireLe;
    }

    public boolean lienValableA(Instant instant) {
        return jetonExpireLe != null && jetonExpireLe.isAfter(instant);
    }

    /** Le lien ne sert qu'une fois. */
    public void confirmerEmail() {
        this.emailVerifie = true;
        this.jetonConfirmationHash = null;
        this.jetonExpireLe = null;
    }
}
