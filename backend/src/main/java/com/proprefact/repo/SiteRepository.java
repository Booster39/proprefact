package com.proprefact.repo;

import com.proprefact.domain.Site;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
    List<Site> findByClientIdOrderByNom(Long clientId);
}
