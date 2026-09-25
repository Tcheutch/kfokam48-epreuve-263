package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Presence;
import com.kfokam48.presence.service.projection.Comptage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Presence> findBySessionId(Long sessionId);

    long countByEtudiantId(Long etudiantId);

    /**
     * EF7 — les présences de toute une promotion en UNE requête.
     *
     * <p>Une requête par étudiant ferait soixante allers-retours pour une
     * promotion pleine ; ENF2 demande moins de deux secondes.
     */
    @Query("""
            select new com.kfokam48.presence.service.projection.Comptage(p.etudiant.id, count(p))
            from Presence p
            where p.etudiant.promotion.id = :promotionId
            group by p.etudiant.id
            """)
    List<Comptage> comptageParEtudiant(Long promotionId);
}
