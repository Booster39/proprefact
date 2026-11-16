package com.proprefact.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

/** Lieu d'intervention chez un client (bureaux, immeuble, commerce...). */
@Entity
@Getter
public class Site extends TenantEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    private String nom;
    private String adresse;
    private String codePostal;
    private String ville;

    /** Réservé à JPA. */
    protected Site() {}

    public Site(Client client, String nom, String adresse, String codePostal, String ville) {
        this.client = client;
        this.nom = nom;
        this.adresse = adresse;
        this.codePostal = codePostal;
        this.ville = ville;
    }
}
