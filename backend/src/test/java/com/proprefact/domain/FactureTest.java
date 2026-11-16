package com.proprefact.domain;

import static com.proprefact.domain.Exemples.brouillon;
import static com.proprefact.domain.Exemples.ligne;
import static com.proprefact.domain.Exemples.vendeur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class FactureTest {
    private static final LocalDate JOUR = LocalDate.of(2026, 11, 14);
    private static final Instant INSTANT = Instant.parse("2026-11-14T10:00:00Z");

    @Test
    void lesTotauxAdditionnentLesLignesEtArrondissentLaTvaParTaux() {
        Facture facture = brouillon(ligne("480.00", "20"), ligne("333.33", "20"));

        assertThat(facture.getTotalHt()).isEqualByComparingTo("813.33");
        assertThat(facture.getTotalTva()).isEqualByComparingTo("162.67");
        assertThat(facture.getTotalTtc()).isEqualByComparingTo("976.00");
    }

    @Test
    void laTvaEstCalculeeSeparementPourChaqueTaux() {
        Facture facture = brouillon(ligne("100.00", "20"), ligne("50.00", "10"), ligne("30.00", "20.00"));

        assertThat(facture.basesParTaux()).hasSize(2);
        assertThat(facture.tvaParTaux().get(new BigDecimal("20").stripTrailingZeros()))
                .isEqualByComparingTo("26.00");
        assertThat(facture.tvaParTaux().get(new BigDecimal("10").stripTrailingZeros()))
                .isEqualByComparingTo("5.00");
        assertThat(facture.getTotalTtc()).isEqualByComparingTo("211.00");
    }

    @Test
    void laValidationNumeroteDateEtFigeLesCoordonnees() {
        Facture facture = brouillon(ligne("100.00", "20"));

        facture.valider("F-2026-00001", JOUR, 30, vendeur());

        assertThat(facture.getStatut()).isEqualTo(StatutFacture.VALIDEE);
        assertThat(facture.getNumero()).isEqualTo("F-2026-00001");
        assertThat(facture.getDateEmission()).isEqualTo(JOUR);
        assertThat(facture.getDateEcheance()).isEqualTo(LocalDate.of(2026, 12, 14));
        assertThat(facture.getVendeur().getRaisonSociale()).isEqualTo("Net Plus SARL");
        assertThat(facture.getAcheteur().getRaisonSociale()).isEqualTo("Cabinet Durand");
        assertThat(facture.getVendeurIban()).isEqualTo("FR7630006000011234567890189");
    }

    @Test
    void uneFactureNeSeValideQuUneFois() {
        Facture facture = brouillon(ligne("100.00", "20"));
        facture.valider("F-2026-00001", JOUR, 30, vendeur());

        assertThatThrownBy(() -> facture.valider("F-2026-00002", JOUR, 30, vendeur()))
                .isInstanceOf(TransitionInterditeException.class)
                .hasMessage("Action impossible : la facture est au statut VALIDEE");
        assertThat(facture.getNumero()).isEqualTo("F-2026-00001");
    }

    @Test
    void unBrouillonNePeutEtreNiPayeNiDepose() {
        Facture facture = brouillon(ligne("100.00", "20"));

        assertThatThrownBy(facture::marquerPayee)
                .isInstanceOf(TransitionInterditeException.class)
                .hasMessage("Seule une facture validée peut être marquée payée");
        assertThatThrownBy(() -> facture.demanderDepot("cle", INSTANT))
                .isInstanceOf(TransitionInterditeException.class);
        assertThat(facture.getStatut()).isEqualTo(StatutFacture.BROUILLON);
    }

    @Test
    void unDepotRetenteGardeSaCleEtCompteSesEchecs() {
        Facture facture = brouillon(ligne("100.00", "20"));
        facture.valider("F-2026-00001", JOUR, 30, vendeur());

        facture.demanderDepot("premiere-cle", INSTANT);
        facture.enregistrerDepotEchoue("Plateforme injoignable", INSTANT.plusSeconds(60));
        facture.demanderDepot("autre-cle", INSTANT.plusSeconds(300));

        assertThat(facture.getStatut()).isEqualTo(StatutFacture.DEPOT_EN_COURS);
        assertThat(facture.getCleDepot()).isEqualTo("premiere-cle");
        assertThat(facture.getDepotTentatives()).isEqualTo(1);
        assertThat(facture.getDepotErreur()).isEqualTo("Plateforme injoignable");

        facture.enregistrerDepotReussi("PA-1");

        assertThat(facture.getStatut()).isEqualTo(StatutFacture.DEPOSEE);
        assertThat(facture.getIdDepot()).isEqualTo("PA-1");
        assertThat(facture.getDepotErreur()).isNull();
        assertThat(facture.getDepotProchaineTentative()).isNull();
    }

    @Test
    void unMessageDErreurTropLongEstTronqueALaTailleDeLaColonne() {
        Facture facture = brouillon(ligne("100.00", "20"));
        facture.valider("F-2026-00001", JOUR, 30, vendeur());
        facture.demanderDepot("cle", INSTANT);

        facture.enregistrerDepotEchoue("x".repeat(600), INSTANT);

        assertThat(facture.getDepotErreur()).hasSize(Facture.LONGUEUR_MAX_ERREUR);
    }

    @Test
    void uneMensualiteReprendLeMontantEtLeTauxDuContrat() {
        Contrat contrat = Exemples.contrat(LocalDate.of(2026, 1, 1), null);

        LigneFacture mensualite = LigneFacture.mensualite(contrat, YearMonth.of(2026, 11), "Entretien – novembre 2026");

        assertThat(mensualite.getContrat()).isSameAs(contrat);
        assertThat(mensualite.getPeriode()).isEqualTo(YearMonth.of(2026, 11));
        assertThat(mensualite.getQuantite()).isEqualByComparingTo("1");
        assertThat(mensualite.montantHt()).isEqualByComparingTo("480.00");
        assertThat(mensualite.getTauxTva()).isEqualByComparingTo("20");
    }
}
