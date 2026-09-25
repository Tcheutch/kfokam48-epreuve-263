package com.kfokam48.presence.service;

import com.kfokam48.presence.depot.PresenceRepository;
import com.kfokam48.presence.depot.SessionRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Presence;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.domaine.SourcePresence;
import com.kfokam48.presence.domaine.Utilisateur;
import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF2 — marquer sa présence.
 *
 * <p>L'ordre des vérifications est lui-même une décision : il détermine quel
 * code d'erreur l'étudiant reçoit quand plusieurs cas sont vrais à la fois.
 * Il suit D3 et le contrat.
 *
 * <p>Le contrat imposé n'autorise que {@code 400}, {@code 409} et {@code 410}
 * sur cette opération. Les erreurs qui vaudraient normalement {@code 404} —
 * un étudiant introuvable — sortent donc en {@code 400}, en gardant leur code
 * métier pour que le frontend les distingue.
 */
@Service
public class PresenceService {

    private final PresenceRepository presences;
    private final SessionRepository sessions;
    private final UtilisateurRepository utilisateurs;
    private final Clock horloge;

    public PresenceService(
            PresenceRepository presences,
            SessionRepository sessions,
            UtilisateurRepository utilisateurs,
            Clock horloge) {
        this.presences = presences;
        this.sessions = sessions;
        this.utilisateurs = utilisateurs;
        this.horloge = horloge;
    }

    /** EF2 — produit toujours une présence {@code source = ETUDIANT} (RG14). */
    @Transactional
    public Presence marquerAvecCode(String code, Long etudiantId) {
        if (code == null || code.isBlank() || etudiantId == null) {
            throw new ErreurMetier(CodeErreur.CHAMP_MANQUANT);
        }

        Utilisateur etudiant = utilisateurs
                .findById(etudiantId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.ETUDIANT_INCONNU, HttpStatus.BAD_REQUEST));

        // RG17 — un code inconnu vaut 400, et non 404. C'est contre-intuitif en
        // REST, mais c'est ce que le contrat impose, et il est noté.
        Session session = sessions
                .findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ErreurMetier(CodeErreur.CODE_INCONNU));

        Instant maintenant = Instant.now(horloge);

        // RG3 + RG20 — la clôture est vérifiée avant l'expiration : une session
        // clôturée l'est définitivement, l'expiration n'est qu'un délai.
        if (session.estCloturee()) {
            throw new ErreurMetier(CodeErreur.SESSION_CLOTUREE);
        }
        if (session.codeExpireA(maintenant)) {
            throw new ErreurMetier(CodeErreur.CODE_EXPIRE);
        }

        return enregistrer(session, etudiant, SourcePresence.ETUDIANT, maintenant);
    }

    /**
     * Le cœur commun au marquage par code (EF2) et à l'ajout manuel du
     * formateur (EF8) : mêmes règles d'appartenance et d'unicité, seule la
     * source change.
     */
    @Transactional
    public Presence enregistrer(Session session, Utilisateur etudiant, SourcePresence source, Instant maintenant) {
        // H11 — un code qui fuite hors de la promotion ne doit pas être exploitable.
        Long promotionEtudiant = etudiant.getPromotion() == null ? null : etudiant.getPromotion().getId();
        if (!session.getPromotion().getId().equals(promotionEtudiant)) {
            throw new ErreurMetier(CodeErreur.ETUDIANT_HORS_PROMOTION, HttpStatus.BAD_REQUEST);
        }

        // RG15 — la contrainte UNIQUE en base est le garde-fou ; cette
        // vérification est là pour renvoyer le bon code d'erreur plutôt qu'une
        // violation d'intégrité.
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiant.getId())) {
            throw new ErreurMetier(CodeErreur.DEJA_PRESENT);
        }

        return presences.save(new Presence(session, etudiant, source, maintenant));
    }

    @Transactional(readOnly = true)
    public List<Presence> deLaSession(Long sessionId) {
        if (!sessions.existsById(sessionId)) {
            throw new ErreurMetier(CodeErreur.SESSION_INCONNUE);
        }
        return presences.findBySessionId(sessionId);
    }
}
