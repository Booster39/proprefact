package com.proprefact.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proprefact.domain.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findAllByOrderByRaisonSociale();
}
