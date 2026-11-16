package com.proprefact.domain;

/** E-mail inconnu ou mot de passe faux : le message ne dit jamais lequel des deux. */
public class IdentifiantsIncorrectsException extends RuntimeException {
    public IdentifiantsIncorrectsException() {
        super("Identifiants incorrects");
    }
}
