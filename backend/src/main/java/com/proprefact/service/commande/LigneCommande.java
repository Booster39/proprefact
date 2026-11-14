package com.proprefact.service.commande;

import static com.proprefact.service.commande.Messages.*;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Ligne d'une facture ponctuelle. Les tailles maximales sont celles des colonnes de la base. */
public record LigneCommande(
        @NotBlank(message = OBLIGATOIRE) @Size(max = 500, message = MAX_500)
        String designation,

        @NotNull(message = OBLIGATOIRE)
        @DecimalMin(value = "0.001", message = POSITIF)
        @Digits(integer = 9, fraction = 3, message = TROIS_DECIMALES)
        BigDecimal quantite,

        // La base garde 2 décimales : en accepter plus ferait changer le total à la validation.
        @NotNull(message = OBLIGATOIRE)
        @DecimalMin(value = "0", message = NON_NEGATIF)
        @Digits(integer = 10, fraction = 2, message = DEUX_DECIMALES)
        BigDecimal prixUnitaireHt,

        @NotNull(message = OBLIGATOIRE)
        @DecimalMin(value = "0", message = NON_NEGATIF)
        @DecimalMax(value = "100", message = "100 au plus")
        @Digits(integer = 3, fraction = 2, message = DEUX_DECIMALES)
        BigDecimal tauxTva) {}
