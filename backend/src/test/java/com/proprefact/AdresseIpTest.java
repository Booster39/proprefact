package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Limite de tentatives par adresse IP derrière le proxy de Railway, qui écrit l'adresse du client dans X-Real-IP.
 * Réglages propres à cette classe (limite basse, en-tête lu) : elle a donc son propre contexte Spring et sa propre
 * base, contrairement aux autres tests d'intégration.
 */
@TestPropertySource(properties = {"app.auth.max-requetes-par-ip=3", "app.auth.entete-ip-client=X-Real-IP"})
class AdresseIpTest extends TestIntegration {

    @Test
    void inventerUneAdresseDansXForwardedForNeContournePasLaLimite() throws Exception {
        for (int essai = 1; essai <= 3; essai++) {
            demanderLien("203.0.113.7", "198.51.100." + essai).andExpect(status().isAccepted());
        }
        // Chaque requête prétend venir d'ailleurs : sans effet, seul X-Real-IP (écrit par le proxy) compte.
        demanderLien("203.0.113.7", "198.51.100.99").andExpect(status().isTooManyRequests());
        demanderLien("203.0.113.7", null).andExpect(status().isTooManyRequests());
    }

    @Test
    void laLimiteSAppliqueAChaqueClientSeparement() throws Exception {
        for (int essai = 1; essai <= 3; essai++) {
            demanderLien("203.0.113.20", null).andExpect(status().isAccepted());
        }
        demanderLien("203.0.113.20", null).andExpect(status().isTooManyRequests());
        // Un autre client derrière le même proxy n'est pas bloqué.
        demanderLien("203.0.113.21", null).andExpect(status().isAccepted());
    }

    @Test
    void unEnTeteQuiNEstPasUneAdresseNeCreePasDeNouveauCompteur() throws Exception {
        // Valeurs malformées : on retombe sur l'adresse de la connexion (la même ici), que le client ne choisit pas.
        String[] inventees = {"203.0.113.30, 10.0.0.1", "inconnu", "", "203.0.113.31 ", "x".repeat(60)};
        int acceptees = 0;
        for (String valeur : inventees) {
            int code = demanderLien(valeur, null).andReturn().getResponse().getStatus();
            acceptees += code == 202 ? 1 : 0;
        }
        // « 203.0.113.31 » (espaces retirés) est une vraie adresse : compteur à part. Les quatre autres partagent
        // l'adresse de la connexion, limitée à 3.
        assertThat(acceptees).isEqualTo(4);
    }

    private ResultActions demanderLien(String xRealIp, String xForwardedFor) throws Exception {
        var requete = post("/api/auth/renvoi-confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"personne@exemple.fr\"}")
                .header("X-Real-IP", xRealIp);
        if (xForwardedFor != null) {
            requete.header("X-Forwarded-For", xForwardedFor);
        }
        return mvc.perform(requete);
    }
}
