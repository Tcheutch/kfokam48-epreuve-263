package com.kfokam48.presence.api.dto;

/**
 * Corps imposé de {@code POST /api/exercices} :
 * {@code required [sessionId, etudiantId, lien]}.
 */
public record DeposerExerciceRequete(Long sessionId, Long etudiantId, String lien) {}
