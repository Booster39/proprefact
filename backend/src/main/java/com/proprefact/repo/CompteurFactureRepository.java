package com.proprefact.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.proprefact.domain.CompteurFacture;

import jakarta.persistence.LockModeType;

public interface CompteurFactureRepository extends JpaRepository<CompteurFacture, Long> {
    /** Verrou en écriture : deux validations simultanées ne peuvent pas obtenir le même numéro. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CompteurFacture> findByAnnee(int annee);
}
