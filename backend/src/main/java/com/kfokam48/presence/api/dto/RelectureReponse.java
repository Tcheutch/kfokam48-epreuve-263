package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.StatutRelecture;
import java.time.Instant;

/**
 * Réponse de {@code POST /api/relectures/{id}}.
 *
 * <p>Ne porte aucun nom : c'est la réponse rendue au relecteur, qui sait déjà
 * qui il est. L'anonymat de RG8 se joue côté auteur, dans {@code MonExercice}.
 */
public record RelectureReponse(
        Long id,
        Long exerciceId,
        StatutRelecture statut,
        Integer note,
        String commentaire,
        Instant rendueAt,
        boolean modifiable) {

    public static RelectureReponse de(Relecture relecture) {
        return new RelectureReponse(
                relecture.getId(),
                relecture.getExercice().getId(),
                relecture.getStatut(),
                relecture.getNote(),
                relecture.getCommentaire(),
                relecture.getRendueAt(),
                !relecture.getExercice().getSession().estCloturee());
    }
}
