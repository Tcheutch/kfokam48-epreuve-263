package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.StatutExercice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Exercice> findByEtudiantIdOrderByDeposeAtDesc(Long etudiantId);

    long countByEtudiantId(Long etudiantId);

    /** RG22 — les exercices restés sans relecteur, à rejouer quand quelqu'un arrive. */
    List<Exercice> findBySessionIdAndStatut(Long sessionId, StatutExercice statut);
}
