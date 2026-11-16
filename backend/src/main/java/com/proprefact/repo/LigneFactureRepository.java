package com.proprefact.repo;

import com.proprefact.domain.LigneFacture;
import java.time.YearMonth;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LigneFactureRepository extends JpaRepository<LigneFacture, Long> {
    @Query("select l.contrat.id from LigneFacture l where l.periode = :periode and l.contrat is not null")
    Set<Long> findContratsDejaFactures(YearMonth periode);
}
