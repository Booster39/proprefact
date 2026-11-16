package com.proprefact;

import com.proprefact.service.Messagerie;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Remplace l'envoi d'e-mails par une boîte en mémoire que les tests peuvent relire. */
class Courrier implements Messagerie {
    private final Map<String, List<String>> recus = new ConcurrentHashMap<>();

    @Override
    public void envoyer(String destinataire, String sujet, String texte) {
        recus.computeIfAbsent(destinataire, d -> new CopyOnWriteArrayList<>()).add(sujet + "\n" + texte);
    }

    String dernier(String destinataire) {
        return recus.getOrDefault(destinataire, List.of("")).getLast();
    }

    int nombre(String destinataire) {
        return recus.getOrDefault(destinataire, List.of()).size();
    }
}
