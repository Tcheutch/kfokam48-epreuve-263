package com.kfokam48.presence.service;

import com.kfokam48.presence.depot.PresenceRepository;
import com.kfokam48.presence.depot.RelectureRepository;
import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.Presence;
import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.Utilisateur;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * EF4 — le tirage du relecteur.
 *
 * <p>RG7 (Q7) : le relecteur est tiré au sort par le système, parmi les
 * étudiants <strong>présents à cette session</strong>. RG5 (Q5) : jamais
 * l'auteur. RG6 (Q6) : un seul relecteur, et la contrainte
 * {@code UNIQUE (exercice_id)} le garantit en base.
 *
 * <p>RG22 (hypothèse H3) — <strong>le cas que le client n'a pas envisagé</strong> :
 * si l'auteur est le seul présent, l'ensemble des relecteurs éligibles est vide.
 * L'exercice reste alors au statut {@code DEPOSE}, sans relecture, et le tirage
 * est rejoué à chaque nouvelle présence enregistrée sur la session. Un exercice
 * peut donc légitimement finir sans note ; le tableau du formateur distingue ce
 * cas de « en attente de relecture ».
 */
@Component
public class TirageRelecteur {

    private static final Logger log = LoggerFactory.getLogger(TirageRelecteur.class);

    private final PresenceRepository presences;
    private final RelectureRepository relectures;
    private final Random alea;

    public TirageRelecteur(PresenceRepository presences, RelectureRepository relectures, Random alea) {
        this.presences = presences;
        this.relectures = relectures;
        this.alea = alea;
    }

    /**
     * Tente d'assigner un relecteur. Renvoie vide si personne n'est éligible —
     * ce n'est pas une erreur, c'est RG22.
     */
    public Optional<Relecture> assigner(Exercice exercice, Instant maintenant) {
        // RG6 — jamais deux relecteurs pour un même exercice.
        if (relectures.findByExerciceId(exercice.getId()).isPresent()) {
            return Optional.empty();
        }

        List<Utilisateur> eligibles = eligiblesPour(exercice);
        if (eligibles.isEmpty()) {
            log.debug(
                    "Exercice {} : aucun relecteur éligible, il reste au statut DEPOSE (RG22).",
                    exercice.getId());
            return Optional.empty();
        }

        Utilisateur relecteur = eligibles.get(alea.nextInt(eligibles.size()));
        Relecture relecture = relectures.save(new Relecture(exercice, relecteur, maintenant));
        exercice.marquerEnAttente();
        return Optional.of(relecture);
    }

    /** RG7 + RG5 — les présents de la session, l'auteur exclu. */
    private List<Utilisateur> eligiblesPour(Exercice exercice) {
        Long auteurId = exercice.getEtudiant().getId();
        return presences.findBySessionId(exercice.getSession().getId()).stream()
                .map(Presence::getEtudiant)
                .filter(Utilisateur::estEtudiant)
                .filter(candidat -> !candidat.getId().equals(auteurId))
                .toList();
    }
}
