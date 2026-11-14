package com.proprefact.service.commande;

import static com.proprefact.service.commande.Messages.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Création d'une facture ponctuelle. Les tailles maximales sont celles des colonnes de la base. */
public record FactureCommande(
        @NotNull(message = OBLIGATOIRE) Long clientId,

        @NotEmpty(message = "au moins une ligne") @Valid List<LigneCommande> lignes) {}
