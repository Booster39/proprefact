package com.proprefact;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;

/**
 * Le front-end est servi par Spring Boot à la même adresse que l'API. Les tests utilisent le petit index.html et le
 * fichier .js de src/test/resources/static, pour ne pas dépendre d'une compilation préalable du vrai front-end.
 */
class FrontTest extends TestIntegration {
    private static final String PAGE = "page-de-test";

    @Test
    void uneAdresseDeLApplicationRenvoieLaPageDAccueil() throws Exception {
        for (String adresse : new String[] {"/factures", "/confirmation?jeton=abc-123", "/contrats/12"}) {
            mvc.perform(get(adresse))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/html"))
                    .andExpect(content().string(containsString(PAGE)))
                    // Sans revalidation, un navigateur garderait après une mise à jour une page qui charge des
                    // fichiers .js disparus.
                    .andExpect(header().string("Cache-Control", "no-cache"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "same-origin"));
        }
    }

    /** Spring Boot renvoie l'accueil vers index.html (renvoi interne, que MockMvc n'exécute pas). */
    @Test
    void laRacineRenvoieLaPageDAccueil() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(PAGE)))
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    void unFichierAuNomHacheEstServiSansConnexionEtGardeEnCache() throws Exception {
        mvc.perform(get("/main-ESSAI123.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("essai")))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("immutable")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void unFichierAbsentNeRenvoiePasLaPageDAccueil() throws Exception {
        mvc.perform(get("/absent-ABC.js")).andExpect(status().isNotFound());
        mvc.perform(get("/images/absent.png")).andExpect(status().isNotFound());
    }

    @Test
    void lApiResteProtegee() throws Exception {
        mvc.perform(get("/api/clients")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/inexistant")).andExpect(status().isUnauthorized());
    }

    /** Vérification de santé de Clever Cloud : accessible sans connexion, sans aucun détail, seul point d'Actuator. */
    @Test
    void laSanteEstPubliqueEtSansDetail() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
        for (String adresse : new String[] {"/actuator", "/actuator/env", "/actuator/health/db", "/actuator/beans"}) {
            mvc.perform(get(adresse))
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().string(not(containsString(PAGE))));
        }
    }

    @Test
    void uneAdresseInconnueDeLApiNeRenvoiePasLaPageDAccueil() throws Exception {
        String jeton = inscrire("front@exemple.fr", "616161616", "Front SAS");
        for (String adresse : new String[] {"/api/inexistant", "/api", "/api/clients/1/inexistant"}) {
            mvc.perform(get(adresse).header("Authorization", "Bearer " + jeton))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(not(containsString(PAGE))));
        }
    }
}
