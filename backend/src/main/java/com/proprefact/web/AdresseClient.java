package com.proprefact.web;

import com.proprefact.proprietes.ProprietesApp;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Adresse IP du client, clé de la limite de tentatives par IP.
 *
 * <p>Derrière un proxy (Railway), l'adresse de la connexion est celle du proxy : tous les clients la partageraient.
 * On lit alors UN en-tête que le proxy écrit lui-même ({@code app.auth.entete-ip-client}, X-Real-IP chez Railway).
 * X-Forwarded-For n'est jamais lu : un proxy qui le complète au lieu de le remplacer laisse le client y inventer
 * une adresse à chaque requête et contourner la limite (défaut corrigé dans nginx au lot 21).
 *
 * <p>Sans en-tête configuré, seule l'adresse de la connexion compte : un en-tête envoyé par le client est ignoré.
 */
@Component
class AdresseClient {
    /** Chiffres, lettres hexadécimales, points et deux-points : IPv4 ou IPv6, rien d'autre (ni liste, ni texte). */
    private static final Pattern ADRESSE_IP = Pattern.compile("[0-9A-Fa-f:.]{2,45}");

    private final String entete;

    AdresseClient(ProprietesApp proprietes) {
        this.entete = proprietes.auth().enteteIpClient().strip();
    }

    String de(HttpServletRequest requete) {
        if (entete.isEmpty()) {
            return requete.getRemoteAddr();
        }
        String valeur = requete.getHeader(entete);
        if (valeur != null && ADRESSE_IP.matcher(valeur.strip()).matches()) {
            return valeur.strip();
        }
        // En-tête absent ou malformé : la requête n'est pas passée par le proxy comme prévu. On retombe sur
        // l'adresse de la connexion, que le client ne choisit pas.
        return requete.getRemoteAddr();
    }
}
