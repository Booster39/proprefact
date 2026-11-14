package com.proprefact.service.commande;

import static com.proprefact.service.commande.Messages.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Identifiants de connexion. Les tailles maximales sont celles des colonnes de la base. */
public record ConnexionCommande(
        @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
        String email,

        @NotBlank(message = OBLIGATOIRE) String motDePasse) {}
