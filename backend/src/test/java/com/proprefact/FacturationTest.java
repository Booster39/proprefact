package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.mustangproject.Invoice;
import org.mustangproject.ZUGFeRD.ZUGFeRDImporter;
import org.mustangproject.ZUGFeRD.ZUGFeRDInvoiceImporter;
import org.springframework.http.MediaType;

/** Des contrats à la facture Factur-X : génération, validation, numérotation, données refusées. */
class FacturationTest extends TestIntegration {
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
        assertThat(JsonPath.<String>read(generees, "$[0].periode")).isEqualTo("2026-11");
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
        byte[] pdf = mvc.perform(
                        get("/api/factures/" + facture + "/facturx").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        String xml = new String(new ZUGFeRDImporter(new ByteArrayInputStream(pdf)).getRawXML(), StandardCharsets.UTF_8);
        ZUGFeRDInvoiceImporter lecteur = new ZUGFeRDInvoiceImporter(new ByteArrayInputStream(pdf));
        assertThat(xml)
                .contains("urn:cen.eu:en16931:2017")
                .contains("552100554")
                .contains("732829320");
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
        assertThat(JsonPath.<String>read(
                        envoyer(post("/api/factures/" + ponctuelle + "/valider"), jeton, null), "$.numero"))
                .endsWith("-00002");
    }

    @Test
    void uneFactureValideeNeChangePlusQuandLaFicheClientChange() throws Exception {
        String jeton = inscrire("fige@fige.test", "666666664", "Propreté Figée");
        int client = id(envoyer(post("/api/clients"), jeton, """
                {"raisonSociale":"Ancien Nom SA","siren":"777777772","adresse":"1 rue Ancienne","codePostal":"31000","ville":"Toulouse"}"""));
        String ligne = """
                {"clientId":%d,"lignes":[{"designation":"Vitrerie","quantite":1,"prixUnitaireHt":100,"tauxTva":20}]}""".formatted(client);
        int brouillon = id(envoyer(post("/api/factures"), jeton, ligne));
        int validee = id(envoyer(post("/api/factures"), jeton, ligne));
        envoyer(post("/api/factures/" + validee + "/valider"), jeton, null);
        byte[] avant = telecharger(validee, jeton);

        envoyer(put("/api/clients/" + client), jeton, """
                {"raisonSociale":"Nouveau Nom SAS","siren":"888888880","adresse":"9 avenue Neuve","codePostal":"33000","ville":"Bordeaux"}""");

        // Le fichier émis est rendu à l'identique, avec les coordonnées de l'époque.
        byte[] apres = telecharger(validee, jeton);
        assertThat(apres).isEqualTo(avant);
        String xml =
                new String(new ZUGFeRDImporter(new ByteArrayInputStream(apres)).getRawXML(), StandardCharsets.UTF_8);
        assertThat(xml)
                .contains("Ancien Nom SA", "777777772", "1 rue Ancienne")
                .doesNotContain("Nouveau Nom SAS", "888888880");
        mvc.perform(get("/api/factures/" + validee).header("Authorization", "Bearer " + jeton))
                .andExpect(jsonPath("$.client").value("Ancien Nom SA"));

        // Le brouillon, lui, prend les coordonnées à jour au moment de sa validation.
        envoyer(post("/api/factures/" + brouillon + "/valider"), jeton, null);
        String xmlNouveau = new String(
                new ZUGFeRDImporter(new ByteArrayInputStream(telecharger(brouillon, jeton))).getRawXML(),
                StandardCharsets.UTF_8);
        assertThat(xmlNouveau).contains("Nouveau Nom SAS", "888888880").doesNotContain("Ancien Nom SA");
    }

    @Test
    void desValidationsSimultaneesNumerotentSansDoublonNiTrou() throws Exception {
        String jeton = inscrire("simultane@simultane.test", "727272727", "Propreté Simultanée");
        int client = id(envoyer(post("/api/clients"), jeton, """
                {"raisonSociale":"Client","siren":"123456789","adresse":"a","codePostal":"75001","ville":"Paris"}"""));
        List<Integer> brouillons = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            brouillons.add(id(envoyer(post("/api/factures"), jeton, """
                    {"clientId":%d,"lignes":[{"designation":"Vitrerie","quantite":1,"prixUnitaireHt":100,"tauxTva":20}]}""".formatted(client))));
        }

        // Aucune facture n'a encore été validée cette année : le compteur lui-même n'existe pas.
        List<String> numeros = new CopyOnWriteArrayList<>();
        try (var executeur = java.util.concurrent.Executors.newFixedThreadPool(brouillons.size())) {
            for (int brouillon : brouillons) {
                executeur.submit(() -> {
                    String validee = envoyer(post("/api/factures/" + brouillon + "/valider"), jeton, null);
                    numeros.add(JsonPath.<String>read(validee, "$.numero").replaceFirst("F-\\d{4}-", ""));
                    return null;
                });
            }
        }

        assertThat(numeros).containsExactlyInAnyOrder("00001", "00002", "00003", "00004", "00005", "00006");
    }

    /** Inscription complète : création du compte puis confirmation par le lien reçu par e-mail. */
    @Test
    void lesDonneesInvalidesSontRefuseesChampParChamp() throws Exception {
        String jeton = inscrire("valid@valid.test", "909090909", "Propreté Valide");
        mvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"raisonSociale":"","siren":"12","adresse":"a","codePostal":"75001 CEDEX 08","ville":"Paris"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Données invalides : vérifiez les champs signalés"))
                .andExpect(jsonPath("$.erreurs.raisonSociale").value("obligatoire"))
                .andExpect(jsonPath("$.erreurs.siren").value("9 chiffres attendus"))
                .andExpect(jsonPath("$.erreurs.codePostal").value("10 caractères au plus"));

        // Un prix à 3 décimales serait arrondi par la base : le total changerait entre brouillon et validation.
        int client = id(envoyer(post("/api/clients"), jeton, """
                {"raisonSociale":"Client","siren":"123456789","adresse":"a","codePostal":"75001","ville":"Paris"}"""));
        mvc.perform(post("/api/factures")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"clientId":%d,"lignes":[{"designation":"d","quantite":100,"prixUnitaireHt":0.014,"tauxTva":20}]}""".formatted(client)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs['lignes[0].prixUnitaireHt']").value("2 décimales au plus"));
    }

    @Test
    void unContratSeResilieMaisJamaisAvantSonDebut() throws Exception {
        String jeton = inscrire("contrat@contrat.test", "818181818", "Propreté Contrat");
        int client = id(envoyer(post("/api/clients"), jeton, """
                {"raisonSociale":"Client","siren":"123456789","adresse":"a","codePostal":"75001","ville":"Paris"}"""));
        int site = id(envoyer(post("/api/clients/" + client + "/sites"), jeton, """
                {"nom":"Bureaux","adresse":"a","codePostal":"75001","ville":"Paris"}"""));
        String corps = """
                {"siteId":%d,"libelle":"Entretien","montantMensuelHt":100,"tauxTva":20,"dateDebut":"2026-03-15"%s}""";

        mvc.perform(post("/api/contrats")
                        .header("Authorization", "Bearer " + jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps.formatted(site, ",\"dateFin\":\"2026-03-01\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La date de fin précède la date de début"));

        String cree = envoyer(post("/api/contrats"), jeton, corps.formatted(site, ""));
        assertThat(JsonPath.<String>read(cree, "$.site")).isEqualTo("Bureaux");
        assertThat(JsonPath.<String>read(cree, "$.client")).isEqualTo("Client");
        int contrat = id(cree);

        mvc.perform(post("/api/contrats/" + contrat + "/resilier")
                        .param("dateFin", "2026-03-01")
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isBadRequest());
        String resilie =
                envoyer(post("/api/contrats/" + contrat + "/resilier").param("dateFin", "2026-06-30"), jeton, null);
        assertThat(JsonPath.<String>read(resilie, "$.dateFin")).isEqualTo("2026-06-30");
        assertThat(JsonPath.<String>read(resilie, "$.client")).isEqualTo("Client");
    }
}
