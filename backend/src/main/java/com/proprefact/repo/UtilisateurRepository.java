package com.proprefact.repo;

import com.proprefact.domain.Utilisateur;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    Optional<Utilisateur> findByJetonConfirmationHash(String hash);
}
