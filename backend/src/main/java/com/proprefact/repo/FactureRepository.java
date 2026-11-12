package com.proprefact.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.proprefact.domain.Facture;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    @Query("select f from Facture f join fetch f.client order by f.id desc")
    List<Facture> findAllAvecClient();

    @Query("select f from Facture f join fetch f.client left join fetch f.lignes where f.id = :id")
    Optional<Facture> findComplete(Long id);
}
