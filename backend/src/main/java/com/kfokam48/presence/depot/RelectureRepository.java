package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.StatutRelecture;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    Optional<Relecture> findByExerciceId(Long exerciceId);

    List<Relecture> findByRelecteurIdOrderByAssigneeAtDesc(Long relecteurId);

    /** RG11 — ce que compte {@code relecturesEnAttente} dans le tableau. */
    long countByRelecteurIdAndStatut(Long relecteurId, StatutRelecture statut);

    /**
     * RG19 — la moyenne des notes reçues par un étudiant, calculée par l'API et
     * nulle part ailleurs. Seules les relectures rendues comptent.
     */
    @Query("""
            select avg(r.note)
            from Relecture r
            where r.exercice.etudiant.id = :etudiantId
              and r.statut = com.kfokam48.presence.domaine.StatutRelecture.RENDUE
            """)
    Double moyenneDesNotesRecues(Long etudiantId);
}
