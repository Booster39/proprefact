package com.proprefact.repo;

import com.proprefact.domain.CompteurFacture;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CompteurFactureRepository extends JpaRepository<CompteurFacture, Long> {
    /** Verrou en écriture : deux validations simultanées ne peuvent pas obtenir le même numéro. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CompteurFacture> findByAnnee(int annee);
}
