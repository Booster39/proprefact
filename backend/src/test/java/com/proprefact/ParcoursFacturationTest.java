package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.proprefact.service.DepotService;
import com.proprefact.service.Messagerie;
import com.proprefact.service.PlateformeAgreee;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mustangproject.Invoice;
import org.mustangproject.ZUGFeRD.ZUGFeRDImporter;
import org.mustangproject.ZUGFeRD.ZUGFeRDInvoiceImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BaseDeTest.class)
class ParcoursFacturationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Courrier courrier;

    /** Remplace l'envoi d'e-mails par une boîte en mémoire que les tests peuvent relire. */
    static class Courrier implements Messagerie {
        private final Map<String, List<String>> recus = new ConcurrentHashMap<>();

        @Override
        public void envoyer(String destinataire, String sujet, String texte) {
            recus.computeIfAbsent(destinataire, d -> new CopyOnWriteArrayList<>())
                    .add(sujet + "\n" + texte);
        }

        String dernier(String destinataire) {
            return recus.getOrDefault(destinataire, List.of("")).getLast();
        }

        int nombre(String destinataire) {
            return recus.getOrDefault(destinataire, List.of()).size();
        }
    }

    @Autowired
    PlateformeDeTest plateforme;

    @Autowired
    DepotService depots;

    /** Plateforme agréée de test : idempotente comme l'exige le contrat, avec des pannes programmables. */
    static class PlateformeDeTest implements PlateformeAgreee {
        final Map<String, String> depotsParCle = new ConcurrentHashMap<>();
        final List<String> clesRecues = new CopyOnWriteArrayList<>();
        volatile int pannesAvantEnvoi;
        volatile int pannesApresEnvoi;

        @Override
        public String deposer(String cleIdempotence, String numeroFacture, byte[] facturX) {
            clesRecues.add(cleIdempotence);
            if (pannesAvantEnvoi > 0) {
                pannesAvantEnvoi--;
                throw new IllegalStateException("Plateforme injoignable");
            }
            String id = depotsParCle.computeIfAbsent(cleIdempotence, cle -> "PA-" + (depotsParCle.size() + 1));
            if (pannesApresEnvoi > 0) {
                pannesApresEnvoi--;
                throw new IllegalStateException("Réponse perdue");
            }
            return id;
        }
    }

    @Test
    void unDepotEnEchecEstRejoueSansDoublon() throws Exception {
        String jeton = inscrire("depot@depot.test", "246813579", "Propreté Dépôt");
        int client = id(envoyer(post("/api/clients"), jeton, """
                {"raisonSociale":"Client Dépôt","siren":"112233445","adresse":"1 rue D","codePostal":"44000","ville":"Nantes"}"""));
        String corps = """
                {"clientId":%d,"lignes":[{"designation":"Vitrerie","quantite":1,"prixUnitaireHt":100,"tauxTva":20}]}""".formatted(client);
        int avantDepots = plateforme.depotsParCle.size();

        // 1. Plateforme injoignable : la demande est conservée, la facture reste « dépôt en cours ».
        int f1 = id(envoyer(post("/api/factures"), jeton, corps));
        envoyer(post("/api/factures/" + f1 + "/valider"), jeton, null);
        plateforme.pannesAvantEnvoi = 2;
        String enCours = envoyer(post("/api/factures/" + f1 + "/deposer"), jeton, null);
        assertThat(JsonPath.<String>read(enCours, "$.statut")).isEqualTo("DEPOT_EN_COURS");
        assertThat(JsonPath.<String>read(enCours, "$.depotErreur")).isEqualTo("Plateforme injoignable");
        mvc.perform(post("/api/factures/" + f1 + "/payee").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isConflict());

        // La relance automatique attend l'échéance, puis retente ; au second essai réussi, c'est déposé.
        assertThat(depots.relancer(Instant.now())).isZero();
        assertThat(depots.relancer(Instant.now().plus(Duration.ofMinutes(2)))).isEqualTo(1);
        assertThat(JsonPath.<String>read(envoyer(get("/api/factures/" + f1), jeton, null), "$.statut"))
                .isEqualTo("DEPOT_EN_COURS");
        assertThat(depots.relancer(Instant.now().plus(Duration.ofHours(1)))).isEqualTo(1);
        String deposee = envoyer(get("/api/factures/" + f1), jeton, null);
        assertThat(JsonPath.<String>read(deposee, "$.statut")).isEqualTo("DEPOSEE");
        assertThat(JsonPath.<String>read(deposee, "$.idDepot")).startsWith("PA-");
        assertThat(JsonPath.<Object>read(deposee, "$.depotErreur")).isNull();
        assertThat(depots.relancer(Instant.now().plus(Duration.ofDays(1)))).isZero();

        // 2. La plateforme a reçu la facture mais sa réponse s'est perdue : le nouvel essai, fait par
        //    l'utilisateur, réutilise la même clé et n'ajoute aucun dépôt.
        int f2 = id(envoyer(post("/api/factures"), jeton, corps));
        envoyer(post("/api/factures/" + f2 + "/valider"), jeton, null);
        plateforme.pannesApresEnvoi = 1;
        assertThat(JsonPath.<String>read(envoyer(post("/api/factures/" + f2 + "/deposer"), jeton, null), "$.statut"))
                .isEqualTo("DEPOT_EN_COURS");
        assertThat(JsonPath.<String>read(envoyer(post("/api/factures/" + f2 + "/deposer"), jeton, null), "$.statut"))
                .isEqualTo("DEPOSEE");

        // Trois essais pour la première facture, deux pour la seconde, mais deux dépôts seulement.
        assertThat(plateforme.depotsParCle.size() - avantDepots).isEqualTo(2);
        List<String> cles =
                plateforme.clesRecues.subList(plateforme.clesRecues.size() - 5, plateforme.clesRecues.size());
        assertThat(cles.subList(0, 3)).containsOnly(cles.get(0));
        assertThat(cles.subList(3, 5)).containsOnly(cles.get(3)).doesNotContain(cles.get(0));

        // 3. Une facture déjà déposée ne peut pas l'être une seconde fois.
        mvc.perform(post("/api/factures/" + f2 + "/deposer").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isConflict());
        assertThat(plateforme.depotsParCle.size() - avantDepots).isEqualTo(2);
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        PlateformeDeTest plateformeDeTest() {
            return new PlateformeDeTest();
        }

        @Bean
        @Primary
        Courrier courrier() {
            return new Courrier();
        }
    }

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

    /** Inscription complète : création du compte puis confirmation par le lien reçu par e-mail. */
    private String inscrire(String email, String siren, String raisonSociale) throws Exception {
        demanderInscription(email, siren, raisonSociale, "mot-de-passe-solide");
        return confirmer(jetonRecu(email));
    }

    private void demanderInscription(String email, String siren, String raisonSociale, String motDePasse)
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

    private String confirmer(String jetonConfirmation) throws Exception {
        String reponse = mvc.perform(post("/api/auth/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jeton\":\"%s\"}".formatted(jetonConfirmation)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(reponse, "$.jeton");
    }

    private String jetonRecu(String email) {
        Matcher lien = Pattern.compile("jeton=([\\w-]+)").matcher(courrier.dernier(email));
        assertThat(lien.find()).as("lien de confirmation envoyé à " + email).isTrue();
        return lien.group(1);
    }

    private org.springframework.test.web.servlet.ResultActions connecter(String email, String motDePasse)
            throws Exception {
        return mvc.perform(post("/api/auth/connexion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"motDePasse\":\"%s\"}".formatted(email, motDePasse)));
    }

    private String envoyer(MockHttpServletRequestBuilder requete, String jeton, String json) throws Exception {
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

    private byte[] telecharger(int facture, String jeton) throws Exception {
        return mvc.perform(get("/api/factures/" + facture + "/facturx").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private static int id(String json) {
        return JsonPath.read(json, "$.id");
    }
}
