package com.proprefact.service;

/** E-mail à envoyer une fois la transaction en cours validée. */
record EmailAEnvoyer(String destinataire, String sujet, String texte) {}
