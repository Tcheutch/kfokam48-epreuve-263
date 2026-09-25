package com.kfokam48.presence.service;

import com.kfokam48.presence.depot.ExerciceRepository;
import com.kfokam48.presence.depot.RelectureRepository;
import com.kfokam48.presence.depot.SessionRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.domaine.Utilisateur;
import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF3 — déposer, puis remplacer, le lien de son exercice.
 *
 * <p>Deux règles contre-intuitives sont portées ici :
 * <ul>
 *   <li>RG12 (Q12) — le dépôt reste ouvert <strong>après l'expiration du
 *       code</strong>, jusqu'à la clôture. « Certains n'ont pas de connexion le
 *       soir même. » C'est la raison d'être de la distinction entre expiration
 *       et clôture (H1) ;</li>
 *   <li>H10 — la présence n'est pas exigée pour déposer. Seul le <em>tirage du
 *       relecteur</em> exige la présence, et du côté du relecteur (Q7).</li>
 * </ul>
 */
@Service
public class ExerciceService {

    private final ExerciceRepository exercices;
    private final SessionRepository sessions;
    private final UtilisateurRepository utilisateurs;
    private final RelectureRepository relectures;
    private final ValidateurLien validateurLien;
    private final TirageRelecteur tirageRelecteur;
    private final Clock horloge;

    public ExerciceService(
            ExerciceRepository exercices,
            SessionRepository sessions,
            UtilisateurRepository utilisateurs,
            RelectureRepository relectures,
            ValidateurLien validateurLien,
            TirageRelecteur tirageRelecteur,
            Clock horloge) {
        this.exercices = exercices;
        this.sessions = sessions;
        this.utilisateurs = utilisateurs;
        this.relectures = relectures;
        this.validateurLien = validateurLien;
        this.tirageRelecteur = tirageRelecteur;
        this.horloge = horloge;
    }

    /**
     * EF3 — dépose l'exercice. Le contrat n'autorise que {@code 400} et
     * {@code 409} : une session ou un étudiant introuvable sort donc en
     * {@code 400}, avec son code métier.
     */
    @Transactional
    public Exercice deposer(Long sessionId, Long etudiantId, String lien) {
        if (sessionId == null || etudiantId == null) {
            throw new ErreurMetier(CodeErreur.CHAMP_MANQUANT);
        }

        Session session = sessions
                .findById(sessionId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.SESSION_INCONNUE, HttpStatus.BAD_REQUEST));
        Utilisateur etudiant = utilisateurs
                .findById(etudiantId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.ETUDIANT_INCONNU, HttpStatus.BAD_REQUEST));

        String lienValide = validateurLien.valider(lien);

        // RG20 — seule la clôture arrête le dépôt. L'expiration du code n'y
        // change rien (RG12) : c'est volontaire, et c'est testé.
        if (session.estCloturee()) {
            throw new ErreurMetier(CodeErreur.SESSION_CLOTUREE);
        }

        if (exercices.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw new ErreurMetier(CodeErreur.EXERCICE_DEJA_DEPOSE);
        }

        Instant maintenant = Instant.now(horloge);
        Exercice exercice = exercices.save(new Exercice(session, etudiant, lienValide, maintenant));

        // EF4 — le tirage suit immédiatement le dépôt. S'il n'aboutit pas,
        // l'exercice reste au statut DEPOSE : ce n'est pas une erreur (RG22).
        tirageRelecteur.assigner(exercice, maintenant);
        return exercice;
    }

    /**
     * RG13 (Q13 + hypothèse H4) — « tant que personne n'a commencé à relire »
     * est interprété comme « tant que la relecture n'a pas été <em>rendue</em> ».
     *
     * <p>L'autre lecture — « tant qu'aucun relecteur n'est assigné » — viderait
     * Q13 de son sens : le tirage a lieu au dépôt, donc la fenêtre serait
     * toujours vide.
     */
    @Transactional
    public Exercice remplacerLien(Long exerciceId, String lien) {
        Exercice exercice = exercices
                .findById(exerciceId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.EXERCICE_INCONNU));

        String lienValide = validateurLien.valider(lien);

        if (exercice.getSession().estCloturee()) {
            throw new ErreurMetier(CodeErreur.SESSION_CLOTUREE);
        }

        Optional<Relecture> relecture = relectures.findByExerciceId(exerciceId);
        if (relecture.isPresent() && relecture.get().estRendue()) {
            throw new ErreurMetier(CodeErreur.RELECTURE_DEJA_RENDUE);
        }

        exercice.remplacerLien(lienValide);
        return exercice;
    }
}
