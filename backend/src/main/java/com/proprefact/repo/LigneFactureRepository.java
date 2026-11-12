package com.proprefact.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.proprefact.domain.LigneFacture;

public interface LigneFactureRepository extends JpaRepository<LigneFacture, Long> {
    @Query("select l.contrat.id from LigneFacture l where l.periode = :periode and l.contrat is not null")
    Set<Long> findContratsDejaFactures(String periode);
}
