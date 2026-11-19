package com.proprefact.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.proprefact.proprietes.ProprietesApp;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AdresseClientTest {

    @Test
    void sansEnTeteConfigureLesEnTetesEnvoyesParLeClientSontIgnores() {
        MockHttpServletRequest requete = requete("203.0.113.5");
        requete.addHeader("X-Real-IP", "198.51.100.1");
        requete.addHeader("X-Forwarded-For", "198.51.100.2");

        assertThat(adresseClient("").de(requete)).isEqualTo("203.0.113.5");
    }

    @Test
    void avecEnTeteConfigureSeulCetEnTeteEstLu() {
        MockHttpServletRequest requete = requete("10.0.0.2");
        requete.addHeader("X-Real-IP", "2001:db8::1");
        requete.addHeader("X-Forwarded-For", "198.51.100.2");

        assertThat(adresseClient("X-Real-IP").de(requete)).isEqualTo("2001:db8::1");
    }

    @Test
    void enTeteAbsentOuMalformeDonneLAdresseDeLaConnexion() {
        assertThat(adresseClient("X-Real-IP").de(requete("10.0.0.2"))).isEqualTo("10.0.0.2");

        MockHttpServletRequest liste = requete("10.0.0.2");
        liste.addHeader("X-Real-IP", "198.51.100.1, 198.51.100.2");
        assertThat(adresseClient("X-Real-IP").de(liste)).isEqualTo("10.0.0.2");
    }

    private static MockHttpServletRequest requete(String adresseConnexion) {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.setRemoteAddr(adresseConnexion);
        return requete;
    }

    private static AdresseClient adresseClient(String entete) {
        return new AdresseClient(new ProprietesApp(
                "http://localhost",
                new ProprietesApp.Jwt("x".repeat(32), 1),
                new ProprietesApp.Mail("a@b.fr"),
                new ProprietesApp.Auth(30, 5, entete),
                new ProprietesApp.Depot(false, 60000)));
    }
}
