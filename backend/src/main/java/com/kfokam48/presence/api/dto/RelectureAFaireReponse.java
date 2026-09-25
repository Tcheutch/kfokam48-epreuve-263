package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.StatutRelecture;

/**
 * Ce qu'un relecteur voit de sa mission (RG11).
 *
 * <p>Il connaît le nom de l'auteur : l'anonymat de Q8 est <strong>simple et non
 * réciproque</strong> (hypothèse H8). Q8 ne protège que l'auteur — « pas le nom
 * du relecteur » — et rien n'est dit sur ce que voit le relecteur. Décision
 * conforme à ce que le client a écrit, et non à ce qu'on suppose qu'il voulait.
 *
 * <p>Le lien est relu à chaque affichage : il peut changer sous le relecteur
 * tant qu'il n'a pas rendu sa note (RG13, H4).
 */
public record RelectureAFaireReponse(
        Long id,
        Long exerciceId,
        String sessionTitre,
        String lien,
        String auteurNom,
        StatutRelecture statut,
        Integer note,
        String commentaire,
        boolean modifiable) {

    public static RelectureAFaireReponse de(Relecture relecture) {
        var exercice = relecture.getExercice();
        return new RelectureAFaireReponse(
                relecture.getId(),
                exercice.getId(),
                exercice.getSession().getTitre(),
                exercice.getLien(),
                exercice.getEtudiant().getNom(),
                relecture.getStatut(),
                relecture.getNote(),
                relecture.getCommentaire(),
                !exercice.getSession().estCloturee());
    }
}
