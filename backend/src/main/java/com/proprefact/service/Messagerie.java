package com.proprefact.service;

/** Envoi des e-mails transactionnels (confirmation d'adresse, etc.). */
public interface Messagerie {
    void envoyer(String destinataire, String sujet, String texte);
}
