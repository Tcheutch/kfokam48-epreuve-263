package com.kfokam48.presence.service;

import com.kfokam48.presence.depot.PromotionRepository;
import com.kfokam48.presence.depot.SessionRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Promotion;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.domaine.Utilisateur;
import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    /** Garde-fou : au-delà, c'est que l'alphabet ou la base a un problème, pas la chance. */
    private static final int TIRAGES_MAX = 20;

    private final SessionRepository sessions;
    private final PromotionRepository promotions;
    private final UtilisateurRepository utilisateurs;
    private final GenerateurCode generateurCode;
    private final Clock horloge;

    public SessionService(
            SessionRepository sessions,
            PromotionRepository promotions,
            UtilisateurRepository utilisateurs,
            GenerateurCode generateurCode,
            Clock horloge) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.utilisateurs = utilisateurs;
        this.generateurCode = generateurCode;
        this.horloge = horloge;
    }

    /**
     * EF1 — ouvre une session et tire son code de présence.
     *
     * <p>RG2 : l'expiration est posée par le constructeur de {@link Session}, à
     * l'ouverture plus quinze minutes.
     *
     * <p>Une promotion inconnue sort en {@code 400} et non en {@code 404} : le
     * contrat imposé n'autorise que {@code 400} sur cette opération.
     */
    @Transactional
    public Session ouvrir(String titre, Long promotionId) {
        if (titre == null || titre.isBlank() || promotionId == null) {
            throw new ErreurMetier(CodeErreur.CHAMP_MANQUANT);
        }

        Promotion promotion = promotions
                .findById(promotionId)
                .orElseThrow(() -> new ErreurMetier(CodeErreur.PROMOTION_INCONNUE, HttpStatus.BAD_REQUEST));

        Utilisateur formateur = utilisateurs
                .findFirstByPromotionIdAndRole(promotionId, Role.FORMATEUR)
                .orElse(null);

        Instant maintenant = Instant.now(horloge);
        Session session = new Session(titre.trim(), tirerUnCodeLibre(), maintenant, promotion, formateur);
        return sessions.save(session);
    }

    /** RG21 — le code est unique ; on retire tant que celui tiré est déjà pris. */
    private String tirerUnCodeLibre() {
        for (int essai = 0; essai < TIRAGES_MAX; essai++) {
            String code = generateurCode.genere();
            if (!sessions.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Impossible de tirer un code de présence libre après " + TIRAGES_MAX + " essais.");
    }

    @Transactional(readOnly = true)
    public Session parId(Long id) {
        return sessions.findById(id).orElseThrow(() -> new ErreurMetier(CodeErreur.SESSION_INCONNUE));
    }
}
