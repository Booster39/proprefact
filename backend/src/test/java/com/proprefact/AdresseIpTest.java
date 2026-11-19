package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Limite de tentatives par adresse IP derrière le proxy de Clever Cloud, qui ajoute l'adresse du client à la fin de
 * X-Forwarded-For (après ce que le client a pu y écrire lui-même).
 * Réglages propres à cette classe (limite basse, en-tête lu) : elle a donc son propre contexte Spring et sa propre
 * base, contrairement aux autres tests d'intégration.
 */
@TestPropertySource(properties = {"app.auth.max-requetes-par-ip=3", "app.auth.entete-ip-client=X-Forwarded-For"})
class AdresseIpTest extends TestIntegration {

    @Test
    void inventerUneAdresseDansXForwardedForNeContournePasLaLimite() throws Exception {
        for (int essai = 1; essai <= 3; essai++) {
            demanderLien("203.0.113.7", "198.51.100." + essai).andExpect(status().isAccepted());
        }
        // Chaque requête prétend venir d'ailleurs : sans effet, seule l'adresse ajoutée par le proxy compte.
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
        String[] inventees = {"10.0.0.1, inconnu", "inconnu", "", "203.0.113.31 ", "x".repeat(60)};
        int acceptees = 0;
        for (String valeur : inventees) {
            int code = mvc.perform(requete(valeur)).andReturn().getResponse().getStatus();
            acceptees += code == 202 ? 1 : 0;
        }
        // « 203.0.113.31 » (espaces retirés) est une vraie adresse : compteur à part. Les quatre autres partagent
        // l'adresse de la connexion, limitée à 3.
        assertThat(acceptees).isEqualTo(4);
    }

    /**
     * Requête telle que le proxy la transmet : ce que le client a mis dans X-Forwarded-For (s'il a mis quelque
     * chose), puis l'adresse réelle du client, ajoutée par le proxy.
     */
    private ResultActions demanderLien(String adresseReelle, String inventeeParLeClient) throws Exception {
        String valeur = inventeeParLeClient == null ? adresseReelle : inventeeParLeClient + ", " + adresseReelle;
        return mvc.perform(requete(valeur));
    }

    private static MockHttpServletRequestBuilder requete(String xForwardedFor) {
        return post("/api/auth/renvoi-confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"personne@exemple.fr\"}")
                .header("X-Forwarded-For", xForwardedFor);
    }
}
