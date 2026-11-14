package com.proprefact.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class EnvoiEmails {
    private final Messagerie messagerie;

    EnvoiEmails(Messagerie messagerie) {
        this.messagerie = messagerie;
    }

    /**
     * Après validation de la transaction : un lien n'est envoyé que si le compte est réellement enregistré, et un
     * serveur de messagerie lent ne retient pas de connexion à la base. Si l'envoi échoue, l'erreur est écrite
     * dans le journal et l'utilisateur peut redemander le lien.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void envoyer(EmailAEnvoyer email) {
        messagerie.envoyer(email.destinataire(), email.sujet(), email.texte());
    }
}
