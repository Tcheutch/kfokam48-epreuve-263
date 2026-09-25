package com.kfokam48.presence.service.projection;

/** Un compte par étudiant, renvoyé par une requête agrégée. */
public record Comptage(Long etudiantId, Long valeur) {}
