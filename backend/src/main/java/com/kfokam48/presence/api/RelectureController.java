package com.kfokam48.presence.api;

import com.kfokam48.presence.api.dto.RelectureReponse;
import com.kfokam48.presence.api.dto.RendreRelectureRequete;
import com.kfokam48.presence.service.RelectureService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relectures")
public class RelectureController {

    private final RelectureService relectureService;

    public RelectureController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    /** Le contrat impose un {@code 200}, et non un {@code 201} : l'objet existait déjà. */
    @PostMapping("/{id}")
    public RelectureReponse rendre(
            @PathVariable Long id, @RequestBody(required = false) RendreRelectureRequete requete) {
        RendreRelectureRequete corps =
                requete == null ? new RendreRelectureRequete(null, null, null) : requete;
        return RelectureReponse.de(
                relectureService.rendre(id, corps.note(), corps.commentaire(), corps.relecteurId()));
    }
}
