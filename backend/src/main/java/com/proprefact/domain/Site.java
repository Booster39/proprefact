package com.proprefact.domain;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

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
