package com.proprefact.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class LimiteurTentativesTest {
    private static final Duration FENETRE = Duration.ofMinutes(15);

    private final AtomicReference<Instant> maintenant = new AtomicReference<>(Instant.parse("2026-11-13T10:00:00Z"));
    private final LimiteurTentatives limiteur = new LimiteurTentatives(new Clock() {
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return maintenant.get(); }
    });

    @Test
    void refuseAuDelaDuMaximumPuisRepartApresLaFenetre() {
        assertThat(limiteur.tenter("ip:1", 2, FENETRE)).isTrue();
        assertThat(limiteur.atteinte("ip:1", 2, FENETRE)).isFalse();
        assertThat(limiteur.tenter("ip:1", 2, FENETRE)).isTrue();
        assertThat(limiteur.atteinte("ip:1", 2, FENETRE)).isTrue();
        assertThat(limiteur.tenter("ip:1", 2, FENETRE)).isFalse();
        assertThat(limiteur.tenter("ip:2", 2, FENETRE)).as("les clés sont indépendantes").isTrue();

        maintenant.set(maintenant.get().plus(FENETRE));
        assertThat(limiteur.atteinte("ip:1", 2, FENETRE)).isFalse();
        assertThat(limiteur.tenter("ip:1", 2, FENETRE)).isTrue();
    }

    @Test
    void oublierRemetLeCompteurAZero() {
        limiteur.tenter("echecs:a", 1, FENETRE);
        assertThat(limiteur.atteinte("echecs:a", 1, FENETRE)).isTrue();
        limiteur.oublier("echecs:a");
        assertThat(limiteur.atteinte("echecs:a", 1, FENETRE)).isFalse();
    }
}
