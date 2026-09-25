package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.StatutExercice;

/**
 * Réponse imposée de {@code POST /api/exercices} : {@code required [id, statut]}.
 */
public record ExerciceDeposeReponse(Long id, StatutExercice statut) {

    public static ExerciceDeposeReponse de(Exercice exercice) {
        return new ExerciceDeposeReponse(exercice.getId(), exercice.getStatut());
    }
}
