package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.Session;
import java.time.Instant;

/**
 * Réponse imposée de {@code POST /api/sessions} :
 * {@code required [id, code, ouvertureAt, expirationAt]}.
 *
 * <p>Un DTO, et non l'entité : aucune entité JPA n'est exposée en JSON (B3).
 */
public record SessionCreeeReponse(Long id, String code, Instant ouvertureAt, Instant expirationAt) {

    public static SessionCreeeReponse de(Session session) {
        return new SessionCreeeReponse(
                session.getId(), session.getCode(), session.getOuvertureAt(), session.getExpirationAt());
    }
}
