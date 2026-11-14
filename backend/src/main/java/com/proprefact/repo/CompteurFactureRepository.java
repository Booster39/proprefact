package com.proprefact.repo;

import com.proprefact.domain.CompteurFacture;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CompteurFactureRepository extends JpaRepository<CompteurFacture, Long> {
    /** Verrou en écriture : deux validations simultanées ne peuvent pas obtenir le même numéro. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CompteurFacture> findByAnnee(int annee);

    /**
     * Crée le compteur de l'année s'il n'existe pas ; sans effet sinon, même en cas d'appels simultanés. Sans cela,
     * le verrou de {@link #findByAnnee} ne protège pas la toute première facture de l'année. Requête native, donc
     * non filtrée par entreprise : l'identifiant de l'entreprise est toujours passé explicitement.
     */
    @Modifying
    @Query(value = """
                    insert into compteur_facture (entreprise_id, annee, dernier_numero)
                    values (:entrepriseId, :annee, 0)
                    on conflict (entreprise_id, annee) do nothing""", nativeQuery = true)
    void creerSiAbsent(long entrepriseId, int annee);
}
