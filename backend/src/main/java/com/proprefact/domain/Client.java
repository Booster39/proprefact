package com.proprefact.domain;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Client extends TenantEntity {
    private String raisonSociale;
    /** Mention obligatoire sur les factures électroniques. */
    private String siren;

    private String tvaIntra;
    private String adresse;
    private String codePostal;
    private String ville;
    private String email;
}
