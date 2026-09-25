package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Utilisateur;

public record UtilisateurReponse(Long id, String nom, Role role, Long promotionId) {

    public static UtilisateurReponse de(Utilisateur utilisateur) {
        return new UtilisateurReponse(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getRole(),
                utilisateur.getPromotion() == null ? null : utilisateur.getPromotion().getId());
    }
}
