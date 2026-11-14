package com.proprefact.proprietes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Réglages propres à l'application (préfixe « app » de application.properties). Une valeur absente ou invalide
 * empêche le démarrage, avec un message qui nomme la propriété.
 */
@Validated
@ConfigurationProperties("app")
public record ProprietesApp(
        /** Adresse publique du front-end, utilisée dans les liens envoyés par e-mail. */
        @NotBlank String urlPublique,
        @NotNull @Valid Jwt jwt,
        @NotNull @Valid Mail mail,
        @NotNull @Valid Auth auth,
        @DefaultValue @Valid Depot depot) {

    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @Positive long dureeHeures) {}

    public record Mail(@NotBlank String expediteur) {}

    /** Limites sur les points d'entrée d'authentification, par fenêtre de 15 minutes. */
    public record Auth(
            @Positive int maxRequetesParIp, @Positive int maxEchecsParEmail) {}

    /** Relance automatique des dépôts restés en cours, et son intervalle en millisecondes. */
    public record Depot(
            @DefaultValue("true") boolean relanceActive,
            @DefaultValue("60000") @Positive long relanceMs) {}
}
