package com.proprefact.domain;

/** L'objet demandé n'existe pas, ou pas pour cette entreprise. */
public class IntrouvableException extends RuntimeException {
    public IntrouvableException(String message) {
        super(message);
    }
}
