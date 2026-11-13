package com.proprefact.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proprefact.domain.FactureDocument;

public interface FactureDocumentRepository extends JpaRepository<FactureDocument, Long> {
    Optional<FactureDocument> findByFactureId(Long factureId);
}
