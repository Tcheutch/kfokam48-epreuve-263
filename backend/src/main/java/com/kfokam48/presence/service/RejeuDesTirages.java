package com.kfokam48.presence.service;

import com.kfokam48.presence.depot.ExerciceRepository;
import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.StatutExercice;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RG22 — le rattrapage des exercices restés sans relecteur, rejoué quand un
 * étudiant arrive sur la séance.
 *
 * <p><strong>Pourquoi cette classe existe (issue #22).</strong> Ce rejeu vivait
 * dans la transaction qui enregistre la présence. Quand il échouait — deux
 * arrivées simultanées tirant pour le même exercice — son échec annulait la
 * présence, une écriture qui n'avait rien à voir avec lui. Le client l'a vu
 * ainsi : « il n'y en a qu'un seul qui apparaît dans ma liste ».
 *
 * <p>Marquer une présence et rattraper un exercice orphelin sont deux
 * opérations distinctes : l'une est un fait que l'étudiant a produit, l'autre
 * une commodité du système. Elles ne doivent pas partager leur sort. Le rejeu
 * s'exécute donc dans sa <em>propre</em> transaction, <strong>après</strong> le
 * commit de la présence — après, et non en parallèle, pour que le tirage voie
 * l'étudiant qui vient d'arriver, qui est précisément le relecteur qu'on
 * cherchait.
 */
@Component
public class RejeuDesTirages {

    private static final Logger log = LoggerFactory.getLogger(RejeuDesTirages.class);

    private final ExerciceRepository exercices;
    private final TirageRelecteur tirageRelecteur;
    private final Clock horloge;

    public RejeuDesTirages(ExerciceRepository exercices, TirageRelecteur tirageRelecteur, Clock horloge) {
        this.exercices = exercices;
        this.tirageRelecteur = tirageRelecteur;
        this.horloge = horloge;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejouerPour(Long sessionId) {
        Instant maintenant = Instant.now(horloge);

        List<Long> candidats = exercices.findBySessionIdAndStatut(sessionId, StatutExercice.DEPOSE).stream()
                .map(Exercice::getId)
                .toList();

        for (Long exerciceId : candidats) {
            // Issue #22 — on reprend la ligne SOUS VERROU, et on revérifie son
            // statut : entre la lecture ci-dessus et maintenant, un autre
            // rattrapage concurrent a pu lui trouver un relecteur.
            exercices.verrouillerPourTirage(exerciceId)
                    .filter(exercice -> exercice.getStatut() == StatutExercice.DEPOSE)
                    .ifPresent(exercice -> tirageRelecteur.assigner(exercice, maintenant));
        }
    }

    /**
     * Le rejeu est une commodité : son échec ne doit jamais remonter à
     * l'appelant. Un exercice qui n'a pas trouvé de relecteur cette fois-ci en
     * retrouvera un à la prochaine arrivée.
     *
     * <p>L'attrape est chez l'appelant et non ici, volontairement : un
     * {@code try/catch} autour d'un appel à {@code this.rejouerPour(...)}
     * contournerait le proxy Spring, la transaction {@code REQUIRES_NEW} ne
     * démarrerait pas, et les entités seraient détachées — le rejeu échouerait
     * alors en silence, ce que l'attrape cacherait précisément.
     */
    void journaliserEchec(Long sessionId, RuntimeException e) {
        log.warn(
                "Rejeu des tirages en échec pour la séance {} : la présence reste enregistrée, "
                        + "le rattrapage sera retenté à la prochaine arrivée (RG22).",
                sessionId,
                e);
    }
}
