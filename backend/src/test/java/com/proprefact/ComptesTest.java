package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Inscription, confirmation de l'adresse et connexion. */
class ComptesTest extends TestIntegration {
    @Test
    void unCompteNeSOuvreQuApresConfirmationDeLEmail() throws Exception {
        demanderInscription("nouveau@conf.test", "123456782", "Propreté Conf", "mot-de-passe-solide");
        connecter("nouveau@conf.test", "mot-de-passe-solide").andExpect(status().isForbidden());

        // Un lien inventé est refusé ; le vrai lien ne sert qu'une fois.
        mvc.perform(post("/api/auth/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jeton\":\"jeton-invente\"}"))
                .andExpect(status().isBadRequest());
        String lien = jetonRecu("nouveau@conf.test");
        confirmer(lien);
        mvc.perform(post("/api/auth/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jeton\":\"%s\"}".formatted(lien)))
                .andExpect(status().isBadRequest());
        connecter("nouveau@conf.test", "mot-de-passe-solide").andExpect(status().isOk());

        // Renvoi du lien : même réponse pour un compte en attente, déjà confirmé ou inconnu.
        demanderInscription("attente@conf.test", "123456782", "En attente", "mot-de-passe-solide");
        String premier = jetonRecu("attente@conf.test");
        for (String email : List.of("attente@conf.test", "nouveau@conf.test", "inconnu@conf.test")) {
            mvc.perform(post("/api/auth/renvoi-confirmation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\"}".formatted(email)))
                    .andExpect(status().isAccepted())
                    .andExpect(content().string(""));
        }
        assertThat(courrier.nombre("inconnu@conf.test")).isZero();
        // Le nouveau lien remplace l'ancien.
        mvc.perform(post("/api/auth/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jeton\":\"%s\"}".formatted(premier)))
                .andExpect(status().isBadRequest());
        confirmer(jetonRecu("attente@conf.test"));
    }

    @Test
    void uneReinscriptionNeReveleNiNeDetourneUnCompteExistant() throws Exception {
        inscrire("titulaire@dup.test", "987654324", "Titulaire SARL");
        int avant = courrier.nombre("titulaire@dup.test");

        // Même réponse que pour une adresse nouvelle (vérifiée dans demanderInscription)...
        demanderInscription("titulaire@dup.test", "111222333", "Usurpateur", "autre-mot-de-passe");
        // ... le titulaire est prévenu, sans lien de confirmation, et son compte est intact.
        assertThat(courrier.nombre("titulaire@dup.test")).isEqualTo(avant + 1);
        assertThat(courrier.dernier("titulaire@dup.test"))
                .contains("déjà un compte")
                .doesNotContain("jeton=");
        connecter("titulaire@dup.test", "autre-mot-de-passe").andExpect(status().isUnauthorized());
        connecter("titulaire@dup.test", "mot-de-passe-solide")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raisonSociale").value("Titulaire SARL"));

        // Utiliser le SIREN d'une entreprise déjà inscrite ne bloque personne.
        String autre = inscrire("autre@dup.test", "987654324", "Même SIREN SAS");
        mvc.perform(get("/api/clients").header("Authorization", "Bearer " + autre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Une inscription jamais confirmée ne bloque pas l'adresse : la suivante la remplace.
        demanderInscription("squat@dup.test", "111222333", "Squatteur", "mot-de-passe-squat");
        demanderInscription("squat@dup.test", "444555666", "Vrai Propriétaire", "mot-de-passe-vrai");
        confirmer(jetonRecu("squat@dup.test"));
        connecter("squat@dup.test", "mot-de-passe-squat").andExpect(status().isUnauthorized());
        connecter("squat@dup.test", "mot-de-passe-vrai")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raisonSociale").value("Vrai Propriétaire"));
    }

    @Test
    void lesEchecsDeConnexionRepetesSontBloques() throws Exception {
        inscrire("cible@limite.test", "135792468", "Cible SARL");
        for (int i = 0; i < 5; i++) {
            connecter("cible@limite.test", "mauvais-mot-de-passe").andExpect(status().isUnauthorized());
        }
        // Au-delà de la limite, même le bon mot de passe est refusé jusqu'à la fin de la fenêtre.
        connecter("cible@limite.test", "mauvais-mot-de-passe").andExpect(status().isTooManyRequests());
        connecter("cible@limite.test", "mot-de-passe-solide").andExpect(status().isTooManyRequests());
        // Les autres comptes ne sont pas touchés.
        inscrire("voisin@limite.test", "135792468", "Voisin SARL");
        connecter("voisin@limite.test", "mot-de-passe-solide").andExpect(status().isOk());
    }

    @Test
    void lApiExigeUnJetonEtDesDonneesValides() throws Exception {
        mvc.perform(get("/api/clients")).andExpect(status().isUnauthorized());
        String jeton = inscrire("c@c.test", "555555556", "Propreté C");
        mvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"raisonSociale":"Sans SIREN","siren":"123","adresse":"x","codePostal":"x","ville":"x"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"email":"c@c.test","motDePasse":"mauvais-mot-de-passe"}"""))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"email":"c@c.test","motDePasse":"mot-de-passe-solide"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").isNotEmpty());
    }
}
