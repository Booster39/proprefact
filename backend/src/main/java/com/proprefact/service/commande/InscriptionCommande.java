package com.proprefact.service.commande;

import static com.proprefact.service.commande.Messages.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Création du compte d'une entreprise et de son premier utilisateur. Les tailles maximales sont celles des colonnes de la base. */
public record InscriptionCommande(
        @NotBlank(message = OBLIGATOIRE) @Email(message = EMAIL_INVALIDE) @Size(max = 200, message = MAX_200)
        String email,

        @NotBlank(message = OBLIGATOIRE) @Size(min = 10, max = 72, message = "entre 10 et 72 caractères")
        String motDePasse,

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

        @Size(max = 34, message = MAX_34) String iban,
        @Size(max = 11, message = MAX_11) String bic,
        Boolean optionTvaDebits) {}
