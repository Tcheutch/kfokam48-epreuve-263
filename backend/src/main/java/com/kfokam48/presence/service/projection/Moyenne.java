package com.kfokam48.presence.service.projection;

/** Une moyenne par étudiant. {@code valeur} est nulle si aucune note n'a été reçue. */
public record Moyenne(Long etudiantId, Double valeur) {}
