package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base des tests d'intégration : application complète, base PostgreSQL jetable, API appelée comme le ferait le
 * front-end. Toutes les classes filles partagent le même contexte Spring et la même base : chaque test crée donc
 * sa propre entreprise (adresse e-mail et SIREN uniques) pour ne pas dépendre des autres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({BaseDeTest.class, DoublesDeTest.class})
abstract class TestIntegration {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected Courrier courrier;

    /** Inscription complète : création du compte puis confirmation par le lien reçu par e-mail. */
    protected String inscrire(String email, String siren, String raisonSociale) throws Exception {
        demanderInscription(email, siren, raisonSociale, "mot-de-passe-solide");
        return confirmer(jetonRecu(email));
    }

    protected void demanderInscription(String email, String siren, String raisonSociale, String motDePasse)
            throws Exception {
        mvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"email":"%s","motDePasse":"%s","raisonSociale":"%s","siren":"%s",
                 "tvaIntra":"FR00%s","adresse":"5 rue des Lilas","codePostal":"69003","ville":"Lyon",
                 "iban":"FR7630006000011234567890189","bic":"AGRIFRPP"}""".formatted(email, motDePasse, raisonSociale, siren, siren)))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    protected String confirmer(String jetonConfirmation) throws Exception {
        String reponse = mvc.perform(post("/api/auth/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jeton\":\"%s\"}".formatted(jetonConfirmation)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(reponse, "$.jeton");
    }

    protected String jetonRecu(String email) {
        Matcher lien = Pattern.compile("jeton=([\\w-]+)").matcher(courrier.dernier(email));
        assertThat(lien.find()).as("lien de confirmation envoyé à " + email).isTrue();
        return lien.group(1);
    }

    protected ResultActions connecter(String email, String motDePasse) throws Exception {
        return mvc.perform(post("/api/auth/connexion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"motDePasse\":\"%s\"}".formatted(email, motDePasse)));
    }

    protected String envoyer(MockHttpServletRequestBuilder requete, String jeton, String json) throws Exception {
        requete.header("Authorization", "Bearer " + jeton);
        if (json != null) {
            requete.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(requete)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    protected byte[] telecharger(int facture, String jeton) throws Exception {
        return mvc.perform(get("/api/factures/" + facture + "/facturx").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    protected static int id(String json) {
        return JsonPath.read(json, "$.id");
    }
}
