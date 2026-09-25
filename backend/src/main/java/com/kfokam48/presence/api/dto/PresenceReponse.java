package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.Presence;
import com.kfokam48.presence.domaine.SourcePresence;
import java.time.Instant;

/**
 * Réponse imposée de {@code POST /api/presences} :
 * {@code required [id, sessionId, etudiantId, source]}.
 *
 * <p>{@code nom} et {@code marqueeAt} sont des ajouts, prévus au contrat, que
 * l'écran formateur consomme. Les quatre champs imposés restent inchangés.
 */
public record PresenceReponse(
        Long id, Long sessionId, Long etudiantId, String nom, SourcePresence source, Instant marqueeAt) {

    public static PresenceReponse de(Presence presence) {
        return new PresenceReponse(
                presence.getId(),
                presence.getSession().getId(),
                presence.getEtudiant().getId(),
                presence.getEtudiant().getNom(),
                presence.getSource(),
                presence.getMarqueeAt());
    }
}
