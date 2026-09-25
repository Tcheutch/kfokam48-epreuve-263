package com.kfokam48.presence.api;

import com.kfokam48.presence.api.dto.DeposerExerciceRequete;
import com.kfokam48.presence.api.dto.ExerciceDeposeReponse;
import com.kfokam48.presence.api.dto.ExerciceReponse;
import com.kfokam48.presence.api.dto.RemplacerLienRequete;
import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.service.ExerciceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService exerciceService;

    public ExerciceController(ExerciceService exerciceService) {
        this.exerciceService = exerciceService;
    }

    @PostMapping
    public ResponseEntity<ExerciceDeposeReponse> deposer(
            @RequestBody(required = false) DeposerExerciceRequete requete) {
        DeposerExerciceRequete corps =
                requete == null ? new DeposerExerciceRequete(null, null, null) : requete;
        Exercice exercice = exerciceService.deposer(corps.sessionId(), corps.etudiantId(), corps.lien());
        return ResponseEntity.status(HttpStatus.CREATED).body(ExerciceDeposeReponse.de(exercice));
    }

    @PutMapping("/{id}")
    public ExerciceReponse remplacerLien(
            @PathVariable Long id, @RequestBody(required = false) RemplacerLienRequete requete) {
        String lien = requete == null ? null : requete.lien();
        return ExerciceReponse.de(exerciceService.remplacerLien(id, lien));
    }
}
