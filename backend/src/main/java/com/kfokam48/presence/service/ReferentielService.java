package com.kfokam48.presence.service;

import com.kfokam48.presence.depot.PromotionRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Promotion;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Utilisateur;
import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RG1 (Q1) — « l'étudiant choisit son nom dans une liste ». Sans mot de passe,
 * les écrans ont besoin de ces deux listes, et de rien d'autre.
 */
@Service
@Transactional(readOnly = true)
public class ReferentielService {

    private final PromotionRepository promotions;
    private final UtilisateurRepository utilisateurs;

    public ReferentielService(PromotionRepository promotions, UtilisateurRepository utilisateurs) {
        this.promotions = promotions;
        this.utilisateurs = utilisateurs;
    }

    public List<Promotion> promotions() {
        return promotions.findAll();
    }

    public List<Utilisateur> utilisateursDe(Long promotionId, Role role) {
        if (!promotions.existsById(promotionId)) {
            throw new ErreurMetier(CodeErreur.PROMOTION_INCONNUE);
        }
        return role == null
                ? utilisateurs.findByPromotionIdOrderByNomAsc(promotionId)
                : utilisateurs.findByPromotionIdAndRoleOrderByNomAsc(promotionId, role);
    }
}
