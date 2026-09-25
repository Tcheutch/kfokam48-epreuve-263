package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Utilisateur;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    List<Utilisateur> findByPromotionIdOrderByNomAsc(Long promotionId);

    List<Utilisateur> findByPromotionIdAndRoleOrderByNomAsc(Long promotionId, Role role);

    /**
     * Le contrat imposé de {@code POST /api/sessions} ne transmet pas d'identifiant
     * de formateur ; sans authentification (Q1), la session est rattachée au
     * formateur de la promotion. Le champ reste nullable s'il n'y en a pas.
     */
    Optional<Utilisateur> findFirstByPromotionIdAndRole(Long promotionId, Role role);
}
