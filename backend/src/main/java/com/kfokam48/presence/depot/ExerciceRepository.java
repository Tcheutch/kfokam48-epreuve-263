package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.StatutExercice;
import com.kfokam48.presence.service.projection.Comptage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Exercice> findByEtudiantIdOrderByDeposeAtDesc(Long etudiantId);

    long countByEtudiantId(Long etudiantId);

    /** RG22 — les exercices restés sans relecteur, à rejouer quand quelqu'un arrive. */
    List<Exercice> findBySessionIdAndStatut(Long sessionId, StatutExercice statut);

    /** EF7 — les dépôts de toute une promotion en une requête. */
    @Query("""
            select new com.kfokam48.presence.service.projection.Comptage(e.etudiant.id, count(e))
            from Exercice e
            where e.etudiant.promotion.id = :promotionId
            group by e.etudiant.id
            """)
    List<Comptage> comptageParEtudiant(Long promotionId);
}
