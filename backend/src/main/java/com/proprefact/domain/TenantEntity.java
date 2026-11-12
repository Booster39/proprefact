package com.proprefact.domain;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/** Base de toute donnée appartenant à une entreprise cliente. */
@MappedSuperclass
public abstract class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "entreprise_id", nullable = false, updatable = false)
    private Long entrepriseId;

    public Long getId() { return id; }
    public Long getEntrepriseId() { return entrepriseId; }
}
