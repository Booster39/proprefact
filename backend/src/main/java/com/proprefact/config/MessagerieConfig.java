package com.proprefact.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.proprefact.service.Messagerie;

@Configuration
class MessagerieConfig {
    private static final Logger log = LoggerFactory.getLogger(MessagerieConfig.class);

    /**
     * Envoi par SMTP dès que spring.mail.host est configuré. Sinon (poste de développement), le message
     * est seulement écrit dans le journal : ne jamais laisser ce mode actif en production, les liens de
     * confirmation y apparaissent en clair.
     */
    @Bean
    Messagerie messagerie(ObjectProvider<JavaMailSender> smtp, @Value("${spring.mail.host:}") String serveur,
            @Value("${app.mail.expediteur}") String expediteur) {
        JavaMailSender expediteurSmtp = smtp.getIfAvailable();
        if (serveur.isBlank() || expediteurSmtp == null) {
            log.warn("Aucun serveur SMTP configuré (spring.mail.host) : les e-mails sont écrits dans le journal.");
            return (destinataire, sujet, texte) -> log.info("E-mail NON ENVOYÉ à {} – {}\n{}", destinataire, sujet,
                    texte);
        }
        return (destinataire, sujet, texte) -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(expediteur);
            message.setTo(destinataire);
            message.setSubject(sujet);
            message.setText(texte);
            expediteurSmtp.send(message);
        };
    }
}
