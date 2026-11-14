package com.proprefact.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import java.time.Instant;
import lombok.Getter;

/** Fichier Factur-X émis pour une facture validée. Créé une seule fois, jamais modifié. */
@Entity
@Getter
public class FactureDocument extends TenantEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Facture facture;

    private byte[] contenu;
    private String sha256;
    private Instant creeLe;

    protected FactureDocument() {}

    public FactureDocument(Facture facture, byte[] contenu, String sha256, Instant creeLe) {
        this.facture = facture;
        this.contenu = contenu;
        this.sha256 = sha256;
        this.creeLe = creeLe;
    }
}
