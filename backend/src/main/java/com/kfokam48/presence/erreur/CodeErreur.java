package com.kfokam48.presence.erreur;

import org.springframework.http.HttpStatus;

/**
 * Catalogue des codes d'erreur métier, tel que publié dans {@code api/contrat.yaml}.
 *
 * <p>Chaque code porte son statut HTTP <em>habituel</em> et son message français.
 * Le statut peut être surchargé à la levée : le contrat imposé n'autorise pas les
 * mêmes statuts sur toutes les opérations. {@code PROMOTION_INCONNUE} en est
 * l'exemple — 404 sur {@code GET /api/tableau}, mais 400 sur
 * {@code POST /api/sessions}, seul statut d'erreur que le contrat y prévoit.
 */
public enum CodeErreur {

    CHAMP_MANQUANT(HttpStatus.BAD_REQUEST, "Un champ obligatoire est absent ou vide."),
    CODE_INCONNU(HttpStatus.BAD_REQUEST, "Ce code de présence ne correspond à aucune session."),
    CODE_EXPIRE(HttpStatus.GONE, "Le code de présence a expiré."),
    DEJA_PRESENT(HttpStatus.CONFLICT, "Cette présence est déjà enregistrée."),
    TROP_DE_TENTATIVES(HttpStatus.BAD_REQUEST, "Trop de codes erronés. Réessayez dans deux minutes."),
    ETUDIANT_HORS_PROMOTION(HttpStatus.BAD_REQUEST, "Cet étudiant n'appartient pas à la promotion de la session."),
    LIEN_INVALIDE(HttpStatus.BAD_REQUEST, "Le lien doit être une adresse http ou https complète."),
    EXERCICE_DEJA_DEPOSE(HttpStatus.CONFLICT, "Un exercice a déjà été déposé pour cette session."),
    SESSION_CLOTUREE(HttpStatus.CONFLICT, "La session est clôturée : plus aucune modification n'est possible."),
    NOTE_INVALIDE(HttpStatus.BAD_REQUEST, "La note doit être un nombre entier compris entre 0 et 20."),
    AUTO_RELECTURE(HttpStatus.FORBIDDEN, "Un étudiant ne peut pas relire son propre exercice."),
    RELECTURE_DEJA_RENDUE(HttpStatus.CONFLICT, "Cette relecture a été rendue et ne peut plus être modifiée."),
    PROMOTION_INCONNUE(HttpStatus.NOT_FOUND, "Cette promotion est introuvable."),
    SESSION_INCONNUE(HttpStatus.NOT_FOUND, "Cette session est introuvable."),
    ETUDIANT_INCONNU(HttpStatus.NOT_FOUND, "Cet utilisateur est introuvable."),
    EXERCICE_INCONNU(HttpStatus.NOT_FOUND, "Cet exercice est introuvable."),
    RELECTURE_INCONNUE(HttpStatus.NOT_FOUND, "Cette relecture est introuvable."),
    RESSOURCE_INCONNUE(HttpStatus.NOT_FOUND, "Cette adresse n'existe pas sur cette API."),
    ERREUR_INTERNE(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.");

    private final HttpStatus statutHabituel;
    private final String message;

    CodeErreur(HttpStatus statutHabituel, String message) {
        this.statutHabituel = statutHabituel;
        this.message = message;
    }

    public HttpStatus statutHabituel() {
        return statutHabituel;
    }

    public String message() {
        return message;
    }
}
