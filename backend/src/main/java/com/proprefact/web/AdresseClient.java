package com.proprefact.web;

import com.proprefact.proprietes.ProprietesApp;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Adresse IP du client, clé de la limite de tentatives par IP.
 *
 * <p>Derrière un proxy (Clever Cloud), l'adresse de la connexion est celle du proxy : tous les clients la
 * partageraient. On lit alors l'en-tête où le proxy écrit l'adresse du client ({@code app.auth.entete-ip-client},
 * X-Forwarded-For chez Clever Cloud), et seulement sa DERNIÈRE adresse. Le proxy de Clever Cloud complète
 * X-Forwarded-For au lieu de le remplacer : tout ce qui précède la dernière adresse a pu être écrit par le client
 * lui-même, pour inventer une adresse à chaque requête et contourner la limite (défaut corrigé dans nginx au lot
 * 21). Un client peut aussi envoyer l'en-tête sur plusieurs lignes : on prend la dernière.
 *
 * <p>Sans en-tête configuré, seule l'adresse de la connexion compte : un en-tête envoyé par le client est ignoré.
 */
@Component
class AdresseClient {
    /** Chiffres, lettres hexadécimales, points et deux-points : IPv4 ou IPv6, rien d'autre. */
    private static final Pattern ADRESSE_IP = Pattern.compile("[0-9A-Fa-f:.]{2,45}");

    private final String entete;

    AdresseClient(ProprietesApp proprietes) {
        this.entete = proprietes.auth().enteteIpClient().strip();
    }

    String de(HttpServletRequest requete) {
        if (entete.isEmpty()) {
            return requete.getRemoteAddr();
        }
        List<String> lignes = Collections.list(requete.getHeaders(entete));
        if (!lignes.isEmpty()) {
            String derniereLigne = lignes.getLast();
            String derniere =
                    derniereLigne.substring(derniereLigne.lastIndexOf(',') + 1).strip();
            if (ADRESSE_IP.matcher(derniere).matches()) {
                return derniere;
            }
        }
        // En-tête absent ou malformé : la requête n'est pas passée par le proxy comme prévu. On retombe sur
        // l'adresse de la connexion, que le client ne choisit pas.
        return requete.getRemoteAddr();
    }
}
