package com.proprefact.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.proprefact.domain.Contrat;

public interface ContratRepository extends JpaRepository<Contrat, Long> {
    @Query("select c from Contrat c join fetch c.site s join fetch s.client order by s.client.raisonSociale, s.nom, c.id")
    List<Contrat> findAllAvecSiteEtClient();
}
