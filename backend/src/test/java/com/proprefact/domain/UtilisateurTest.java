package com.proprefact.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UtilisateurTest {
    private static final Instant MAINTENANT = Instant.parse("2026-11-16T10:00:00Z");

    private final Utilisateur utilisateur = new Utilisateur("gerant@netplus.test", "hash-1", Exemples.vendeur());

    @Test
    void unLienDeConfirmationExpire() {
        assertThat(utilisateur.lienValableA(MAINTENANT)).as("aucun lien envoyé").isFalse();

        utilisateur.attendreConfirmation("empreinte", MAINTENANT.plusSeconds(3600));

        assertThat(utilisateur.lienValableA(MAINTENANT)).isTrue();
        assertThat(utilisateur.lienValableA(MAINTENANT.plusSeconds(3600))).isFalse();
    }

    @Test
    void laConfirmationActiveLeCompteEtConsommeLeLien() {
        utilisateur.attendreConfirmation("empreinte", MAINTENANT.plusSeconds(3600));

        utilisateur.confirmerEmail();

        assertThat(utilisateur.isEmailVerifie()).isTrue();
        assertThat(utilisateur.getJetonConfirmationHash()).isNull();
        assertThat(utilisateur.lienValableA(MAINTENANT)).isFalse();
    }

    @Test
    void seuleUneInscriptionNonConfirmeePeutEtreRemplacee() {
        utilisateur.remplacerInscription("hash-2");
        assertThat(utilisateur.getMotDePasseHash()).isEqualTo("hash-2");

        utilisateur.confirmerEmail();

        assertThatThrownBy(() -> utilisateur.remplacerInscription("hash-3"))
                .isInstanceOf(TransitionInterditeException.class);
        assertThat(utilisateur.getMotDePasseHash()).isEqualTo("hash-2");
    }
}
