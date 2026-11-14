package com.proprefact.domain;

/** Une action demandée n'est pas permise dans l'état actuel de l'objet. */
public class TransitionInterditeException extends RuntimeException {
    public TransitionInterditeException(String message) {
        super(message);
    }
}
