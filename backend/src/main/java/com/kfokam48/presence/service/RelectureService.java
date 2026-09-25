package com.kfokam48.presence.service;

import com.kfokam48.presence.depot.RelectureRepository;
import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF5 et EF11 — rendre, puis corriger, une relecture.
 *
 * <p><strong>Arbitrage Q10 contre Q15</strong>, justifié en section 7 du cahier
 * des charges : <em>Q10 est retenu</em>. La correction est donc autorisée tant
 * que la session n'est pas clôturée (RG10), et c'est la clôture qui fige la
 * note (RG20). Le {@code 409 RELECTURE_DEJA_RENDUE} du contrat ne tombe donc
 * qu'<em>après</em> la clôture.
 *
 * <p>Conséquence assumée : le formateur qui veut le comportement décrit par Q15
 * — une note définitive dès l'envoi — clôture sa session. L'inverse aurait été
 * impossible.
 */
@Service
public class RelectureService {

    public static final int NOTE_MIN = 0;
    public static final int NOTE_MAX = 20;

    private final RelectureRepository relectures;
    private final Clock horloge;

    public RelectureService(RelectureRepository relectures, Clock horloge) {
        this.relectures = relectures;
        this.horloge = horloge;
    }

    @Transactional
    public Relecture rendre(Long relectureId, BigDecimal note, String commentaire, Long relecteurId) {
        Relecture relecture = relectures
                .findById(relectureId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.RELECTURE_INCONNUE));

        Exercice exercice = relecture.getExercice();
        verifierQuiRelit(relecture, exercice, relecteurId);

        // RG10 + RG20 — la clôture, et elle seule, fige la note.
        if (exercice.getSession().estCloturee()) {
            throw new ErreurMetier(
                    relecture.estRendue() ? CodeErreur.RELECTURE_DEJA_RENDUE : CodeErreur.SESSION_CLOTUREE);
        }

        relecture.rendre(validerNote(note), commentaire, Instant.now(horloge));
        exercice.marquerRelu();
        return relecture;
    }

    /**
     * RG5 (Q5) — « jamais. C'est le principe même. »
     *
     * <p>La garde est double. D'abord le cas de données incohérentes : si le
     * relecteur assigné est l'auteur, on refuse, quoi qu'il arrive. Le tirage
     * l'interdit déjà (RG7), mais une règle aussi catégorique que Q5 ne doit
     * pas reposer sur un seul rempart.
     *
     * <p>Ensuite, si l'appelant se déclare (H12), on vérifie que c'est bien lui
     * le relecteur assigné.
     */
    private void verifierQuiRelit(Relecture relecture, Exercice exercice, Long relecteurId) {
        Long auteurId = exercice.getEtudiant().getId();

        if (relecture.getRelecteur().getId().equals(auteurId)) {
            throw new ErreurMetier(CodeErreur.AUTO_RELECTURE);
        }

        if (relecteurId == null) {
            return;
        }
        if (relecteurId.equals(auteurId)) {
            throw new ErreurMetier(CodeErreur.AUTO_RELECTURE);
        }
        if (!relecteurId.equals(relecture.getRelecteur().getId())) {
            throw new ErreurMetier(CodeErreur.RELECTEUR_NON_ASSIGNE);
        }
    }

    /**
     * RG9 (Q9) — « sur 20, en nombres entiers ». Une valeur décimale est
     * refusée, pas arrondie : arrondir serait inventer une règle que le client
     * n'a pas donnée.
     */
    private int validerNote(BigDecimal note) {
        if (note == null) {
            throw new ErreurMetier(CodeErreur.NOTE_INVALIDE);
        }
        if (note.stripTrailingZeros().scale() > 0) {
            throw new ErreurMetier(CodeErreur.NOTE_INVALIDE);
        }
        int valeur;
        try {
            valeur = note.intValueExact();
        } catch (ArithmeticException e) {
            throw new ErreurMetier(CodeErreur.NOTE_INVALIDE);
        }
        if (valeur < NOTE_MIN || valeur > NOTE_MAX) {
            throw new ErreurMetier(CodeErreur.NOTE_INVALIDE);
        }
        return valeur;
    }

    /** RG11 — ce que le relecteur doit encore faire. */
    @Transactional(readOnly = true)
    public List<Relecture> assigneesA(Long relecteurId) {
        return relectures.findByRelecteurIdOrderByAssigneeAtDesc(relecteurId);
    }
}
