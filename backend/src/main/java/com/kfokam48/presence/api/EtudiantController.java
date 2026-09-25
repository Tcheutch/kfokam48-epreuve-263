package com.kfokam48.presence.api;

import com.kfokam48.presence.api.dto.RelectureAFaireReponse;
import com.kfokam48.presence.service.RelectureService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final RelectureService relectureService;

    public EtudiantController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    /** RG11 — les relectures assignées à cet étudiant, rendues ou non. */
    @GetMapping("/{id}/relectures")
    public List<RelectureAFaireReponse> relectures(@PathVariable Long id) {
        return relectureService.assigneesA(id).stream().map(RelectureAFaireReponse::de).toList();
    }
}
