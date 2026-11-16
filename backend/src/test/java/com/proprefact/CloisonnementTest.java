package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Une entreprise ne voit ni ne modifie les données d'une autre. */
class CloisonnementTest extends TestIntegration {
    @Test
    void uneEntrepriseNeVoitPasLesDonneesDUneAutre() throws Exception {
        String jetonA = inscrire("a@a.test", "111111118", "Propreté A");
        String jetonB = inscrire("b@b.test", "222222226", "Propreté B");
        int clientA = id(envoyer(post("/api/clients"), jetonA, """
                {"raisonSociale":"Client de A","siren":"333333334","adresse":"1 rue A","codePostal":"75001","ville":"Paris"}"""));
        int factureA = id(envoyer(post("/api/factures"), jetonA, """
                {"clientId":%d,"lignes":[{"designation":"Vitrerie","quantite":2,"prixUnitaireHt":90,"tauxTva":20}]}""".formatted(clientA)));

        mvc.perform(get("/api/clients").header("Authorization", "Bearer " + jetonB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/factures").header("Authorization", "Bearer " + jetonB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/factures/" + factureA).header("Authorization", "Bearer " + jetonB))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/clients/" + clientA + "/sites").header("Authorization", "Bearer " + jetonB))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/factures")
                        .header("Authorization", "Bearer " + jetonB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"clientId":%d,"lignes":[{"designation":"x","quantite":1,"prixUnitaireHt":1,"tauxTva":20}]}""".formatted(clientA)))
                .andExpect(status().isNotFound());

        // Chaque entreprise a sa propre séquence de numéros.
        int clientB = id(envoyer(post("/api/clients"), jetonB, """
                {"raisonSociale":"Client de B","siren":"444444442","adresse":"1 rue B","codePostal":"75002","ville":"Paris"}"""));
        int factureB = id(envoyer(post("/api/factures"), jetonB, """
                {"clientId":%d,"lignes":[{"designation":"Vitrerie","quantite":1,"prixUnitaireHt":50,"tauxTva":20}]}""".formatted(clientB)));
        assertThat(JsonPath.<String>read(
                        envoyer(post("/api/factures/" + factureA + "/valider"), jetonA, null), "$.numero"))
                .endsWith("-00001");
        assertThat(JsonPath.<String>read(
                        envoyer(post("/api/factures/" + factureB + "/valider"), jetonB, null), "$.numero"))
                .endsWith("-00001");
    }
}
