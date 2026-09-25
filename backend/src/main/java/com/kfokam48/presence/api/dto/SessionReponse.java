package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.EtatSession;
import com.kfokam48.presence.domaine.Session;
import java.time.Instant;

/**
 * Une session vue de l'extérieur, avec son {@code etat} — les trois notions
 * que le client confond en parlant de « fin de session » (H1) :
 * OUVERTE, EXPIREE, CLOTUREE.
 */
public record SessionReponse(
        Long id,
        String titre,
        String code,
        Instant ouvertureAt,
        Instant expirationAt,
        Instant clotureAt,
        EtatSession etat,
        Long promotionId) {

    public static SessionReponse de(Session session, Instant maintenant) {
        return new SessionReponse(
                session.getId(),
                session.getTitre(),
                session.getCode(),
                session.getOuvertureAt(),
                session.getExpirationAt(),
                session.getClotureAt(),
                session.etatA(maintenant),
                session.getPromotion().getId());
    }
}
