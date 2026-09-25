package com.kfokam48.presence.api;

import com.kfokam48.presence.api.dto.PromotionReponse;
import com.kfokam48.presence.api.dto.UtilisateurReponse;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.service.ReferentielService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
public class ReferentielController {

    private final ReferentielService referentiel;

    public ReferentielController(ReferentielService referentiel) {
        this.referentiel = referentiel;
    }

    @GetMapping
    public List<PromotionReponse> promotions() {
        return referentiel.promotions().stream().map(PromotionReponse::de).toList();
    }

    @GetMapping("/{id}/utilisateurs")
    public List<UtilisateurReponse> utilisateurs(
            @PathVariable Long id, @RequestParam(required = false) Role role) {
        return referentiel.utilisateursDe(id, role).stream().map(UtilisateurReponse::de).toList();
    }
}
