package com.proprefact.config;

import java.io.IOException;
import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sert le front-end Angular compilé (classpath « static/ ») à la même adresse que l'API. Les adresses de
 * l'application (/factures, /confirmation?jeton=…) n'existent que dans le navigateur : elles renvoient index.html,
 * et Angular affiche le bon écran.
 */
@Configuration
class FrontConfig implements WebMvcConfigurer {
    private static final String DOSSIER = "classpath:/static/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Angular met une empreinte du contenu dans le nom de ces fichiers (outputHashing) : une nouvelle version
        // porte un nouveau nom, l'ancien peut donc rester en cache sans jamais être relu.
        registry.addResourceHandler("/*.js", "/*.css", "/media/**")
                .addResourceLocations(DOSSIER)
                .setCacheControl(
                        CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        // index.html (et favicon.ico) gardent leur nom d'une version à l'autre : le navigateur doit revalider à
        // chaque fois, sinon il chargerait après une mise à jour des fichiers .js qui n'existent plus.
        registry.addResourceHandler("/**")
                .addResourceLocations(DOSSIER)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(false)
                .addResolver(new PageOuIndex());
    }

    /** Renvoie le fichier demandé s'il existe, sinon index.html pour une adresse de l'application. */
    static class PageOuIndex extends PathResourceResolver {
        @Override
        protected Resource getResource(String chemin, Resource dossier) throws IOException {
            Resource fichier = super.getResource(chemin, dossier);
            if (fichier != null || !estPageDeLApplication(chemin)) {
                return fichier;
            }
            return super.getResource("index.html", dossier);
        }

        /**
         * Une adresse de l'API ou d'Actuator inconnue doit donner 404, jamais la page d'accueil (le front croirait
         * avoir reçu une réponse) ; de même pour un fichier absent (nom avec extension).
         */
        static boolean estPageDeLApplication(String chemin) {
            String dernierSegment = chemin.substring(chemin.lastIndexOf('/') + 1);
            return !chemin.equals("api")
                    && !chemin.startsWith("api/")
                    && !chemin.equals("actuator")
                    && !chemin.startsWith("actuator/")
                    && !dernierSegment.contains(".");
        }
    }
}
