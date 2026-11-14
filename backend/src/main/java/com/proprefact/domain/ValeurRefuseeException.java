package com.proprefact.domain;

/** Une valeur saisie est refusée par une règle métier ; le message est destiné à l'utilisateur. */
public class ValeurRefuseeException extends RuntimeException {
    public ValeurRefuseeException(String message) {
        super(message);
    }
}
