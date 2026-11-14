package com.proprefact.service.commande;

/** Messages de validation, affichés tels quels à l'utilisateur à côté du nom du champ. */
public final class Messages {
    private Messages() {}

    public static final String SIREN = "\\d{9}";

    public static final String OBLIGATOIRE = "obligatoire";
    public static final String SIREN_INVALIDE = "9 chiffres attendus";
    public static final String EMAIL_INVALIDE = "adresse e-mail invalide";
    public static final String MAX_10 = "10 caractères au plus";
    public static final String MAX_11 = "11 caractères au plus";
    public static final String MAX_20 = "20 caractères au plus";
    public static final String MAX_34 = "34 caractères au plus";
    public static final String MAX_100 = "100 caractères au plus";
    public static final String MAX_200 = "200 caractères au plus";
    public static final String MAX_300 = "300 caractères au plus";
    public static final String MAX_500 = "500 caractères au plus";
    public static final String DEUX_DECIMALES = "2 décimales au plus";
    public static final String TROIS_DECIMALES = "3 décimales au plus";
    public static final String POSITIF = "doit être supérieur à zéro";
    public static final String NON_NEGATIF = "ne peut pas être négatif";
}
