package com.proprefact.service;

import java.util.Locale;

/** Mise en forme des textes saisis avant enregistrement ou comparaison. */
public final class Textes {
    private Textes() {}

    /** Null si la chaîne est absente ou vide, sinon la chaîne sans espaces autour. */
    public static String ouNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    /** Forme unique d'une adresse e-mail : sans espaces autour, en minuscules quelle que soit la langue du serveur. */
    public static String emailNormalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
