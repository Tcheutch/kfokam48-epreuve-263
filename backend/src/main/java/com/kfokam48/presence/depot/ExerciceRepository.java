package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.StatutExercice;
import com.kfokam48.presence.service.projection.Comptage;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Exercice> findByEtudiantIdOrderByDeposeAtDesc(Long etudiantId);

    long countByEtudiantId(Long etudiantId);

    /** RG22 — les exercices restés sans relecteur, à rejouer quand quelqu'un arrive. */
    List<Exercice> findBySessionIdAndStatut(Long sessionId, StatutExercice statut);

    /**
     * Issue #22 — verrouille la ligne de l'exercice le temps du tirage.
     *
     * <p>Deux rattrapages RG22 concurrents lisaient le même exercice au statut
     * {@code DEPOSE} et inséraient chacun une relecture, la seconde violant
     * {@code UNIQUE (exercice_id)}. Le verrou les met en file : le second
     * relit la ligne une fois le premier terminé, la trouve déjà pourvue, et
     * ne fait rien. On préfère empêcher la collision plutôt que la rattraper —
     * un rattrapage laisse passer une transaction annulée, un verrou non.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exercice e where e.id = :id")
    Optional<Exercice> verrouillerPourTirage(Long id);

    /** EF7 — les dépôts de toute une promotion en une requête. */
    @Query("""
            select new com.kfokam48.presence.service.projection.Comptage(e.etudiant.id, count(e))
            from Exercice e
            where e.etudiant.promotion.id = :promotionId
            group by e.etudiant.id
            """)
    List<Comptage> comptageParEtudiant(Long promotionId);
}
