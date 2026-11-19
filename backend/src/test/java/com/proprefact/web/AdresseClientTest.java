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
        requete.addHeader("X-Forwarded-For", "2001:db8::1");
        requete.addHeader("X-Real-IP", "198.51.100.2");

        assertThat(adresseClient("X-Forwarded-For").de(requete)).isEqualTo("2001:db8::1");
    }

    @Test
    void seuleLaDerniereAdresseDeLaListeCompte() {
        // Le client a écrit 198.51.100.1 ; le proxy a ajouté la vraie adresse à la fin.
        MockHttpServletRequest liste = requete("10.0.0.2");
        liste.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.9");
        assertThat(adresseClient("X-Forwarded-For").de(liste)).isEqualTo("203.0.113.9");

        // Même chose quand l'en-tête arrive sur plusieurs lignes : la dernière ligne vient du proxy.
        MockHttpServletRequest lignes = requete("10.0.0.2");
        lignes.addHeader("X-Forwarded-For", "198.51.100.1");
        lignes.addHeader("X-Forwarded-For", "198.51.100.2, 203.0.113.9");
        assertThat(adresseClient("X-Forwarded-For").de(lignes)).isEqualTo("203.0.113.9");
    }

    @Test
    void enTeteAbsentOuMalformeDonneLAdresseDeLaConnexion() {
        assertThat(adresseClient("X-Forwarded-For").de(requete("10.0.0.2"))).isEqualTo("10.0.0.2");

        for (String malformee : new String[] {"inconnu", "", "203.0.113.9, ", "203.0.113.9, inconnu", "x".repeat(60)}) {
            MockHttpServletRequest requete = requete("10.0.0.2");
            requete.addHeader("X-Forwarded-For", malformee);
            assertThat(adresseClient("X-Forwarded-For").de(requete))
                    .as(malformee)
                    .isEqualTo("10.0.0.2");
        }
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
