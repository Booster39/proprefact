package com.proprefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.proprefact.service.DepotService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Dépôt sur la plateforme agréée : rejeu sans doublon après une panne. */
class DepotTest extends TestIntegration {
    @Autowired
    PlateformeDeTest plateforme;

    @Autowired
    DepotService depots;

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
}
