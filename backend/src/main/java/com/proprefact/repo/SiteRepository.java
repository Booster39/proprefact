package com.proprefact.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proprefact.domain.Site;

public interface SiteRepository extends JpaRepository<Site, Long> {
    List<Site> findByClientIdOrderByNom(Long clientId);
}
