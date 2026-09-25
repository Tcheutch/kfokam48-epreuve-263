package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.StatutExercice;
import java.time.Instant;

/** Réponse de {@code PUT /api/exercices/{id}}. */
public record ExerciceReponse(
        Long id, Long sessionId, Long etudiantId, String lien, StatutExercice statut, Instant deposeAt) {

    public static ExerciceReponse de(Exercice exercice) {
        return new ExerciceReponse(
                exercice.getId(),
                exercice.getSession().getId(),
                exercice.getEtudiant().getId(),
                exercice.getLien(),
                exercice.getStatut(),
                exercice.getDeposeAt());
    }
}
