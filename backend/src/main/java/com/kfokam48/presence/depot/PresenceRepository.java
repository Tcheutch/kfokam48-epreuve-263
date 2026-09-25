package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Presence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Presence> findBySessionId(Long sessionId);

    long countByEtudiantId(Long etudiantId);
}
