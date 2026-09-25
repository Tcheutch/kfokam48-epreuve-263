package com.kfokam48.presence.api.dto;

/**
 * Une ligne du tableau du formateur — les six champs imposés par le contrat,
 * ni plus ni moins (Q16).
 *
 * <p>{@code moyenne} est {@code null} quand l'étudiant n'a reçu aucune note.
 * Renvoyer {@code 0} serait dire qu'il a eu zéro : ce n'est pas la même chose,
 * et le formateur prendrait la mauvaise décision.
 */
public record LigneTableauReponse(
        Long etudiantId,
        String nom,
        long presences,
        long exercicesDeposes,
        Double moyenne,
        long relecturesEnAttente) {}
