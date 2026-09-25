package com.kfokam48.presence.domaine;

/** Voir docs/diagrammes/D4_etats_transitions.md. */
public enum StatutExercice {
    /** Déposé, mais aucun relecteur n'a pu être tiré : personne d'autre n'était présent (RG22). */
    DEPOSE,
    /** Un relecteur est assigné et n'a pas encore rendu (RG11). */
    EN_ATTENTE,
    /** La relecture est rendue. */
    RELU
}
