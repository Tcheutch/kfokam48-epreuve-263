package com.kfokam48.presence.service;

import com.kfokam48.presence.api.dto.LigneTableauReponse;
import com.kfokam48.presence.depot.ExerciceRepository;
import com.kfokam48.presence.depot.PresenceRepository;
import com.kfokam48.presence.depot.PromotionRepository;
import com.kfokam48.presence.depot.RelectureRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import com.kfokam48.presence.service.projection.Comptage;
import com.kfokam48.presence.service.projection.Moyenne;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF7 — le tableau du formateur (Q16).
 *
 * <p><strong>C'est ici, et nulle part ailleurs, que la moyenne est calculée</strong>
 * (RG19, contrainte F3). Le frontend affiche la valeur renvoyée ; il ne divise
 * rien. Dupliquer le calcul côté écran, c'est garantir qu'un jour les deux
 * divergeront.
 *
 * <p>Cinq requêtes au total, quelle que soit la taille de la promotion : une
 * pour les étudiants, quatre agrégats groupés. Une requête par étudiant ferait
 * soixante allers-retours, et ENF2 demande moins de deux secondes pour une
 * promotion de soixante.
 */
@Service
@Transactional(readOnly = true)
public class TableauService {

    private final PromotionRepository promotions;
    private final UtilisateurRepository utilisateurs;
    private final PresenceRepository presences;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;

    public TableauService(
            PromotionRepository promotions,
            UtilisateurRepository utilisateurs,
            PresenceRepository presences,
            ExerciceRepository exercices,
            RelectureRepository relectures) {
        this.promotions = promotions;
        this.utilisateurs = utilisateurs;
        this.presences = presences;
        this.exercices = exercices;
        this.relectures = relectures;
    }

    public List<LigneTableauReponse> deLaPromotion(Long promotionId) {
        if (promotionId == null) {
            throw new ErreurMetier(CodeErreur.CHAMP_MANQUANT);
        }
        if (!promotions.existsById(promotionId)) {
            throw new ErreurMetier(CodeErreur.PROMOTION_INCONNUE);
        }

        Map<Long, Long> parPresences = indexer(presences.comptageParEtudiant(promotionId));
        Map<Long, Long> parDepots = indexer(exercices.comptageParEtudiant(promotionId));
        Map<Long, Long> parRelecturesDues = indexer(relectures.relecturesEnAttenteParEtudiant(promotionId));
        Map<Long, Double> parMoyenne = relectures.moyennesParEtudiant(promotionId).stream()
                .filter(m -> m.valeur() != null)
                .collect(Collectors.toMap(Moyenne::etudiantId, Moyenne::valeur));

        // Une ligne par étudiant de la promotion, y compris ceux qui n'ont
        // strictement rien fait : ce sont précisément ceux que le formateur
        // cherche dans ce tableau.
        return utilisateurs.findByPromotionIdAndRoleOrderByNomAsc(promotionId, Role.ETUDIANT).stream()
                .map(etudiant -> new LigneTableauReponse(
                        etudiant.getId(),
                        etudiant.getNom(),
                        parPresences.getOrDefault(etudiant.getId(), 0L),
                        parDepots.getOrDefault(etudiant.getId(), 0L),
                        arrondir(parMoyenne.get(etudiant.getId())),
                        parRelecturesDues.getOrDefault(etudiant.getId(), 0L)))
                .toList();
    }

    private static Map<Long, Long> indexer(List<Comptage> comptages) {
        return comptages.stream().collect(Collectors.toMap(Comptage::etudiantId, Comptage::valeur));
    }

    /**
     * Deux décimales : une moyenne de 15,333333333 n'aide personne. L'arrondi
     * est un choix d'affichage, mais il est fait ici pour que l'API reste la
     * seule source de la valeur — si le front arrondissait, il recommencerait
     * à porter une règle.
     */
    private static Double arrondir(Double moyenne) {
        return moyenne == null
                ? null
                : BigDecimal.valueOf(moyenne).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
