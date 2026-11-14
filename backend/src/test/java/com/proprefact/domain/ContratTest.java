package com.proprefact.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ContratTest {

    @Test
    void unContratEstActifDuMoisDeSonDebutAuMoisDeSaFinInclus() {
        Contrat contrat = contrat(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 9, 1));

        assertThat(contrat.actifPendant(YearMonth.of(2026, 2))).isFalse();
        assertThat(contrat.actifPendant(YearMonth.of(2026, 3))).isTrue();
        assertThat(contrat.actifPendant(YearMonth.of(2026, 6))).isTrue();
        assertThat(contrat.actifPendant(YearMonth.of(2026, 9))).isTrue();
        assertThat(contrat.actifPendant(YearMonth.of(2026, 10))).isFalse();
    }

    @Test
    void unContratSansDateDeFinResteActif() {
        Contrat contrat = contrat(LocalDate.of(2026, 1, 1), null);

        assertThat(contrat.actifPendant(YearMonth.of(2025, 12))).isFalse();
        assertThat(contrat.actifPendant(YearMonth.of(2040, 1))).isTrue();
    }

    @Test
    void laDateDeFinNePeutPasPrecederLeDebut() {
        Contrat contrat = contrat(LocalDate.of(2026, 3, 15), null);

        assertThatThrownBy(() -> contrat.terminerLe(LocalDate.of(2026, 3, 14)))
                .isInstanceOf(ValeurRefuseeException.class)
                .hasMessage("La date de fin précède la date de début");
        assertThat(contrat.getDateFin()).isNull();

        contrat.terminerLe(LocalDate.of(2026, 3, 15));
        assertThat(contrat.getDateFin()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    private static Contrat contrat(LocalDate debut, LocalDate fin) {
        Contrat contrat = new Contrat();
        contrat.setDateDebut(debut);
        contrat.setDateFin(fin);
        return contrat;
    }
}
