package com.proprefact.repo;

import com.proprefact.domain.FactureDocument;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactureDocumentRepository extends JpaRepository<FactureDocument, Long> {
    Optional<FactureDocument> findByFactureId(Long factureId);
}
