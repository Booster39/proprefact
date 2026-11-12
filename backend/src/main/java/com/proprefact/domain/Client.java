package com.proprefact.domain;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Entity;

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
