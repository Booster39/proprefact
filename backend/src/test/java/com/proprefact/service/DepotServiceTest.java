package com.proprefact.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DepotServiceTest {

    @Test
    void leDelaiDoubleAChaqueEchecPuisPlafonneASixHeures() {
        assertThat(DepotService.delaiApres(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(DepotService.delaiApres(2)).isEqualTo(Duration.ofMinutes(2));
        assertThat(DepotService.delaiApres(3)).isEqualTo(Duration.ofMinutes(4));
        assertThat(DepotService.delaiApres(9)).isEqualTo(Duration.ofMinutes(256));
        assertThat(DepotService.delaiApres(10)).isEqualTo(Duration.ofHours(6));
        assertThat(DepotService.delaiApres(50)).isEqualTo(Duration.ofHours(6));
    }
}
