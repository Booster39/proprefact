package com.proprefact.domain;

import jakarta.persistence.Entity;
import lombok.Getter;

@Entity
@Getter
public class Client extends TenantEntity {
    private String raisonSociale;
    /** Mention obligatoire sur les factures électroniques. */
    private String siren;

    private String tvaIntra;
    private String adresse;
    private String codePostal;
    private String ville;
    private String email;

    /** Réservé à JPA. */
    protected Client() {}

    public Client(FicheClient fiche) {
        mettreAJour(fiche);
    }

    /** Sans effet sur les factures déjà validées : elles gardent leur propre copie des coordonnées. */
    public void mettreAJour(FicheClient fiche) {
        this.raisonSociale = fiche.raisonSociale();
        this.siren = fiche.siren();
        this.tvaIntra = fiche.tvaIntra();
        this.adresse = fiche.adresse();
        this.codePostal = fiche.codePostal();
        this.ville = fiche.ville();
        this.email = fiche.email();
    }
}
