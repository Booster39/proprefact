package com.proprefact.config;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Isole les données de chaque entreprise cliente : Hibernate filtre et renseigne
 * automatiquement la colonne entreprise_id de toutes les entités portant @TenantId.
 */
@Component
public class TenantResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    /** Aucune entreprise n'a cet identifiant : une requête non authentifiée ne voit rien. */
    public static final long AUCUNE_ENTREPRISE = 0L;

    public static final String CLAIM_ENTREPRISE = "entreprise_id";

    /** Entreprise imposée pour un traitement de fond, hors de toute requête authentifiée. */
    private final ThreadLocal<Long> imposee = new ThreadLocal<>();

    /**
     * Exécute un traitement au nom d'une entreprise (tâches planifiées). Les transactions doivent être
     * ouvertes à l'intérieur du traitement.
     */
    public void executerPour(long entrepriseId, Runnable traitement) {
        Long precedente = imposee.get();
        imposee.set(entrepriseId);
        try {
            traitement.run();
        } finally {
            if (precedente == null) {
                imposee.remove();
            } else {
                imposee.set(precedente);
            }
        }
    }

    @Override
    public Long resolveCurrentTenantIdentifier() {
        if (imposee.get() != null) {
            return imposee.get();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt
                && jwt.getToken().getClaim(CLAIM_ENTREPRISE) instanceof Number id) {
            return id.longValue();
        }
        return AUCUNE_ENTREPRISE;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
