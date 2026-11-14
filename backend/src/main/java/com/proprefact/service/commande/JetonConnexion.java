package com.proprefact.service.commande;

/** Jeton remis après une connexion réussie, avec le nom de l'entreprise à afficher. */
public record JetonConnexion(String jeton, String raisonSociale) {}
