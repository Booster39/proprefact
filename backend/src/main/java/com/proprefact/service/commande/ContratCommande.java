package com.proprefact.service.commande;

import static com.proprefact.service.commande.Messages.*;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Création d'un contrat récurrent. Les tailles maximales sont celles des colonnes de la base. */
public record ContratCommande(
        @NotNull(message = OBLIGATOIRE) Long siteId,

        @NotBlank(message = OBLIGATOIRE) @Size(max = 300, message = MAX_300)
        String libelle,

        @NotNull(message = OBLIGATOIRE)
        @DecimalMin(value = "0.01", message = POSITIF)
        @Digits(integer = 10, fraction = 2, message = DEUX_DECIMALES)
        BigDecimal montantMensuelHt,

        @NotNull(message = OBLIGATOIRE)
        @DecimalMin(value = "0", message = NON_NEGATIF)
        @DecimalMax(value = "100", message = "100 au plus")
        @Digits(integer = 3, fraction = 2, message = DEUX_DECIMALES)
        BigDecimal tauxTva,

        @NotNull(message = OBLIGATOIRE) LocalDate dateDebut,
        LocalDate dateFin) {}
