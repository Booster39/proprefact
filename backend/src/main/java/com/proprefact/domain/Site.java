package com.proprefact.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/** Lieu d'intervention chez un client (bureaux, immeuble, commerce...). */
@Entity
@Getter
@Setter
public class Site extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    private String nom;
    private String adresse;
    private String codePostal;
    private String ville;
}
