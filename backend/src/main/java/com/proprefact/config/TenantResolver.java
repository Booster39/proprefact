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

    @Override
    public Long resolveCurrentTenantIdentifier() {
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
