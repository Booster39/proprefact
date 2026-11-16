package com.proprefact.domain;

/** Le mot de passe est bon, mais le compte attend encore la confirmation de son adresse e-mail. */
public class AdresseNonConfirmeeException extends RuntimeException {
    public AdresseNonConfirmeeException() {
        super("Adresse e-mail non confirmée : cliquez sur le lien reçu par e-mail");
    }
}
