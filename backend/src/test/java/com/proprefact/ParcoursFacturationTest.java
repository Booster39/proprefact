package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mustangproject.Invoice;
import org.mustangproject.ZUGFeRD.ZUGFeRDImporter;
import org.mustangproject.ZUGFeRD.ZUGFeRDInvoiceImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class ParcoursFacturationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void duContratALaFactureFacturX() throws Exception {
        String jeton = inscrire("gerant@netplus.test", "552100554", "Net Plus SARL");

        int client = id(envoyer(post("/api/clients"), jeton, """
                {"raisonSociale":"Cabinet Durand","siren":"732829320","tvaIntra":"FR44732829320",
                 "adresse":"12 rue de la Paix","codePostal":"69002","ville":"Lyon"}"""));
        int bureaux = id(envoyer(post("/api/clients/" + client + "/sites"), jeton, """
                {"nom":"Bureaux Bellecour","adresse":"12 rue de la Paix","codePostal":"69002","ville":"Lyon"}"""));
        int entrepot = id(envoyer(post("/api/clients/" + client + "/sites"), jeton, """
                {"nom":"Entrepôt Vénissieux","adresse":"3 avenue de l'Industrie","codePostal":"69200","ville":"Vénissieux"}"""));
        envoyer(post("/api/contrats"), jeton, """
                {"siteId":%d,"libelle":"Entretien des bureaux, 3 passages par semaine","montantMensuelHt":480.00,
                 "tauxTva":20,"dateDebut":"2026-01-01"}""".formatted(bureaux));
        envoyer(post("/api/contrats"), jeton, """
                {"siteId":%d,"libelle":"Nettoyage hebdomadaire","montantMensuelHt":333.33,
                 "tauxTva":20,"dateDebut":"2026-03-15"}""".formatted(entrepot));
        envoyer(post("/api/contrats"), jeton, """
                {"siteId":%d,"libelle":"Contrat terminé","montantMensuelHt":100,
                 "tauxTva":20,"dateDebut":"2025-01-01","dateFin":"2026-09-30"}""".formatted(bureaux));

        // Une seule facture pour le client, avec une ligne par contrat actif en novembre.
        String generees = envoyer(post("/api/factures/generer-mensuelles").param("mois", "2026-11"), jeton, null);
        assertThat(JsonPath.<Integer>read(generees, "$.length()")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(generees, "$[0].lignes.length()")).isEqualTo(2);
        assertThat(JsonPath.<Double>read(generees, "$[0].totalHt")).isEqualTo(813.33);
        assertThat(JsonPath.<Double>read(generees, "$[0].totalTva")).isEqualTo(162.67);
        assertThat(JsonPath.<Double>read(generees, "$[0].totalTtc")).isEqualTo(976.00);
        int facture = JsonPath.read(generees, "$[0].id");

        // Relancer la génération ne crée pas de doublon.
        String relance = envoyer(post("/api/factures/generer-mensuelles").param("mois", "2026-11"), jeton, null);
        assertThat(JsonPath.<Integer>read(relance, "$.length()")).isZero();

        // Un brouillon n'a ni numéro ni Factur-X.
        mvc.perform(get("/api/factures/" + facture + "/facturx").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isConflict());

        String validee = envoyer(post("/api/factures/" + facture + "/valider"), jeton, null);
        String numero = JsonPath.read(validee, "$.numero");
        assertThat(numero).matches("F-\\d{4}-00001");
        mvc.perform(post("/api/factures/" + facture + "/valider").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/factures/" + facture).header("Authorization", "Bearer " + jeton))
                .andExpect(status().isConflict());

        // Le PDF embarque le XML structuré, relu ici avec la bibliothèque de référence.
        byte[] pdf = mvc.perform(get("/api/factures/" + facture + "/facturx").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String xml = new String(new ZUGFeRDImporter(new ByteArrayInputStream(pdf)).getRawXML(), StandardCharsets.UTF_8);
        ZUGFeRDInvoiceImporter lecteur = new ZUGFeRDInvoiceImporter(new ByteArrayInputStream(pdf));
        assertThat(xml).contains("urn:cen.eu:en16931:2017").contains("552100554").contains("732829320");
        Invoice relue = lecteur.extractInvoice();
        assertThat(relue.getNumber()).isEqualTo(numero);
        assertThat(relue.getSender().getName()).isEqualTo("Net Plus SARL");
        assertThat(relue.getRecipient().getName()).isEqualTo("Cabinet Durand");
        assertThat(relue.getZFItems()).hasSize(2);
        assertThat(xml).contains("<ram:GrandTotalAmount>976.00</ram:GrandTotalAmount>");

        envoyer(post("/api/factures/" + facture + "/deposer"), jeton, null);
        String payee = envoyer(post("/api/factures/" + facture + "/payee"), jeton, null);
        assertThat(JsonPath.<String>read(payee, "$.statut")).isEqualTo("PAYEE");

        // La numérotation se poursuit sans trou.
        int ponctuelle = id(envoyer(post("/api/factures"), jeton, """
                {"clientId":%d,"lignes":[{"designation":"Remise en état après travaux","quantite":1,
                 "prixUnitaireHt":250,"tauxTva":20}]}""".formatted(client)));
        assertThat(JsonPath.<String>read(envoyer(post("/api/factures/" + ponctuelle + "/valider"), jeton, null),
                "$.numero")).endsWith("-00002");
    }

    @Test
    void uneFactureValideeNeChangePlusQuandLaFicheClientChange() throws Exception {
        String jeton = inscrire("fige@fige.test", "666666664", "Propreté Figée");
        int client = id(envoyer(post("/api/clients"), jeton, """
                {"raisonSociale":"Ancien Nom SA","siren":"777777772","adresse":"1 rue Ancienne","codePostal":"31000","ville":"Toulouse"}"""));
        String ligne = """
                {"clientId":%d,"lignes":[{"designation":"Vitrerie","quantite":1,"prixUnitaireHt":100,"tauxTva":20}]}"""
                .formatted(client);
        int brouillon = id(envoyer(post("/api/factures"), jeton, ligne));
        int validee = id(envoyer(post("/api/factures"), jeton, ligne));
        envoyer(post("/api/factures/" + validee + "/valider"), jeton, null);
        byte[] avant = telecharger(validee, jeton);

        envoyer(put("/api/clients/" + client), jeton, """
                {"raisonSociale":"Nouveau Nom SAS","siren":"888888880","adresse":"9 avenue Neuve","codePostal":"33000","ville":"Bordeaux"}""");

        // Le fichier émis est rendu à l'identique, avec les coordonnées de l'époque.
        byte[] apres = telecharger(validee, jeton);
        assertThat(apres).isEqualTo(avant);
        String xml = new String(new ZUGFeRDImporter(new ByteArrayInputStream(apres)).getRawXML(), StandardCharsets.UTF_8);
        assertThat(xml).contains("Ancien Nom SA", "777777772", "1 rue Ancienne")
                .doesNotContain("Nouveau Nom SAS", "888888880");
        mvc.perform(get("/api/factures/" + validee).header("Authorization", "Bearer " + jeton))
                .andExpect(jsonPath("$.client").value("Ancien Nom SA"));

        // Le brouillon, lui, prend les coordonnées à jour au moment de sa validation.
        envoyer(post("/api/factures/" + brouillon + "/valider"), jeton, null);
        String xmlNouveau = new String(new ZUGFeRDImporter(new ByteArrayInputStream(telecharger(brouillon, jeton)))
                .getRawXML(), StandardCharsets.UTF_8);
        assertThat(xmlNouveau).contains("Nouveau Nom SAS", "888888880").doesNotContain("Ancien Nom SA");
    }

    @Test
    void uneEntrepriseNeVoitPasLesDonneesDUneAutre() throws Exception {
        String jetonA = inscrire("a@a.test", "111111118", "Propreté A");
        String jetonB = inscrire("b@b.test", "222222226", "Propreté B");
        int clientA = id(envoyer(post("/api/clients"), jetonA, """
                {"raisonSociale":"Client de A","siren":"333333334","adresse":"1 rue A","codePostal":"75001","ville":"Paris"}"""));
        int factureA = id(envoyer(post("/api/factures"), jetonA, """
                {"clientId":%d,"lignes":[{"designation":"Vitrerie","quantite":2,"prixUnitaireHt":90,"tauxTva":20}]}"""
                .formatted(clientA)));

        mvc.perform(get("/api/clients").header("Authorization", "Bearer " + jetonB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/factures").header("Authorization", "Bearer " + jetonB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/factures/" + factureA).header("Authorization", "Bearer " + jetonB))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/clients/" + clientA + "/sites").header("Authorization", "Bearer " + jetonB))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/factures").header("Authorization", "Bearer " + jetonB)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"clientId":%d,"lignes":[{"designation":"x","quantite":1,"prixUnitaireHt":1,"tauxTva":20}]}"""
                        .formatted(clientA)))
                .andExpect(status().isNotFound());

        // Chaque entreprise a sa propre séquence de numéros.
        int clientB = id(envoyer(post("/api/clients"), jetonB, """
                {"raisonSociale":"Client de B","siren":"444444442","adresse":"1 rue B","codePostal":"75002","ville":"Paris"}"""));
        int factureB = id(envoyer(post("/api/factures"), jetonB, """
                {"clientId":%d,"lignes":[{"designation":"Vitrerie","quantite":1,"prixUnitaireHt":50,"tauxTva":20}]}"""
                .formatted(clientB)));
        assertThat(JsonPath.<String>read(envoyer(post("/api/factures/" + factureA + "/valider"), jetonA, null),
                "$.numero")).endsWith("-00001");
        assertThat(JsonPath.<String>read(envoyer(post("/api/factures/" + factureB + "/valider"), jetonB, null),
                "$.numero")).endsWith("-00001");
    }

    @Test
    void lApiExigeUnJetonEtDesDonneesValides() throws Exception {
        mvc.perform(get("/api/clients")).andExpect(status().isUnauthorized());
        String jeton = inscrire("c@c.test", "555555556", "Propreté C");
        mvc.perform(post("/api/clients").header("Authorization", "Bearer " + jeton)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"raisonSociale":"Sans SIREN","siren":"123","adresse":"x","codePostal":"x","ville":"x"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"c@c.test","motDePasse":"mauvais-mot-de-passe"}"""))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"c@c.test","motDePasse":"mot-de-passe-solide"}"""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.jeton").isNotEmpty());
    }

    private String inscrire(String email, String siren, String raisonSociale) throws Exception {
        String reponse = mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","motDePasse":"mot-de-passe-solide","raisonSociale":"%s","siren":"%s",
                 "tvaIntra":"FR00%s","adresse":"5 rue des Lilas","codePostal":"69003","ville":"Lyon",
                 "iban":"FR7630006000011234567890189","bic":"AGRIFRPP"}""".formatted(email, raisonSociale, siren, siren)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(reponse, "$.jeton");
    }

    private String envoyer(MockHttpServletRequestBuilder requete, String jeton, String json) throws Exception {
        requete.header("Authorization", "Bearer " + jeton);
        if (json != null) {
            requete.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private byte[] telecharger(int facture, String jeton) throws Exception {
        return mvc.perform(get("/api/factures/" + facture + "/facturx").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private static int id(String json) {
        return JsonPath.read(json, "$.id");
    }
}
