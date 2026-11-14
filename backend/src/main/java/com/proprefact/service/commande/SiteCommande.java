package com.proprefact.service.commande;

import static com.proprefact.service.commande.Messages.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ajout d'un site à un client. Les tailles maximales sont celles des colonnes de la base. */
public record SiteCommande(
        @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
        String nom,

        @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
        String adresse,

        @NotBlank(message = OBLIGATOIRE) @Size(max = 10, message = MAX_10)
        String codePostal,

        @NotBlank(message = OBLIGATOIRE) @Size(max = 100, message = MAX_100)
        String ville) {}
