package com.proprefact.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;

/** Coordonnées du vendeur ou de l'acheteur telles qu'elles étaient à la validation de la facture. */
@Embeddable
@Getter
public class PartieFacture {
    private String raisonSociale;
    private String siren;
    private String tvaIntra;
    private String adresse;
    private String codePostal;
    private String ville;

    protected PartieFacture() {}

    private PartieFacture(
            String raisonSociale, String siren, String tvaIntra, String adresse, String codePostal, String ville) {
        this.raisonSociale = raisonSociale;
        this.siren = siren;
        this.tvaIntra = tvaIntra;
        this.adresse = adresse;
        this.codePostal = codePostal;
        this.ville = ville;
    }

    public static PartieFacture de(Entreprise e) {
        return new PartieFacture(
                e.getRaisonSociale(), e.getSiren(), e.getTvaIntra(), e.getAdresse(), e.getCodePostal(), e.getVille());
    }

    public static PartieFacture de(Client c) {
        return new PartieFacture(
                c.getRaisonSociale(), c.getSiren(), c.getTvaIntra(), c.getAdresse(), c.getCodePostal(), c.getVille());
    }
}
