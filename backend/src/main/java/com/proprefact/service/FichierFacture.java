package com.proprefact.service;

/** Le fichier Factur-X d'une facture validée, avec le numéro qui sert à le nommer. */
public record FichierFacture(String numero, byte[] contenu) {}
