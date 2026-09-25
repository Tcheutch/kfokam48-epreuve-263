package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.TentativeCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TentativeCodeRepository extends JpaRepository<TentativeCode, Long> {

    Optional<TentativeCode> findByEtudiantId(Long etudiantId);
}
