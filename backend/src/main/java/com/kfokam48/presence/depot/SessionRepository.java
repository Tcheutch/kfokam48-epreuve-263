package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Session;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByCode(String code);

    boolean existsByCode(String code);

    List<Session> findByPromotionIdOrderByOuvertureAtDesc(Long promotionId);
}
