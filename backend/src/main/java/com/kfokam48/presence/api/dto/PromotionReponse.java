package com.kfokam48.presence.api.dto;

import com.kfokam48.presence.domaine.Promotion;

public record PromotionReponse(Long id, String nom) {

    public static PromotionReponse de(Promotion promotion) {
        return new PromotionReponse(promotion.getId(), promotion.getNom());
    }
}
