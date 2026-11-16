package com.proprefact.domain;

import jakarta.persistence.Embeddable;

/**
 * Coordonnées bancaires et régime de TVA du vendeur, figés à la validation avec ses coordonnées
 * ({@link PartieFacture}). Enregistrés dans les colonnes vendeur_iban, vendeur_bic et vendeur_option_tva_debits.
 */
@Embeddable
public class ConditionsVendeur {
    private String iban;
    private String bic;
    /** Objet (et non booléen simple) : la colonne est vide tant que la facture est un brouillon. */
    private Boolean optionTvaDebits;

    /** Réservé à JPA. */
    protected ConditionsVendeur() {}

    private ConditionsVendeur(String iban, String bic, boolean optionTvaDebits) {
        this.iban = iban;
        this.bic = bic;
        this.optionTvaDebits = optionTvaDebits;
    }

    public static ConditionsVendeur de(Entreprise entreprise) {
        return new ConditionsVendeur(entreprise.getIban(), entreprise.getBic(), entreprise.isOptionTvaDebits());
    }

    /** Nul si le vendeur n'a pas indiqué de compte : la facture ne propose alors pas de virement. */
    public String getIban() {
        return iban;
    }

    public String getBic() {
        return bic;
    }

    /** Le vendeur a opté pour le paiement de la TVA d'après les débits (mention obligatoire sur la facture). */
    public boolean isOptionTvaDebits() {
        return Boolean.TRUE.equals(optionTvaDebits);
    }
}
