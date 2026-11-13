package com.proprefact.domain;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
@Getter
@Setter
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
}
