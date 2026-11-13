package com.proprefact.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proprefact.domain.Entreprise;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
}
