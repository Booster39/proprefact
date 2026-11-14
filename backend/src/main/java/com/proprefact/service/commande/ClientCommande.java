package com.proprefact.service.commande;

import static com.proprefact.service.commande.Messages.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Création ou modification d'un client. Les tailles maximales sont celles des colonnes de la base. */
public record ClientCommande(
        @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
        String raisonSociale,

        @NotBlank(message = OBLIGATOIRE) @Pattern(regexp = SIREN, message = SIREN_INVALIDE)
        String siren,

        @Size(max = 20, message = MAX_20) String tvaIntra,

        @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
        String adresse,

        @NotBlank(message = OBLIGATOIRE) @Size(max = 10, message = MAX_10)
        String codePostal,

        @NotBlank(message = OBLIGATOIRE) @Size(max = 100, message = MAX_100)
        String ville,

        @Email(message = EMAIL_INVALIDE) @Size(max = 200, message = MAX_200)
        String email) {}
