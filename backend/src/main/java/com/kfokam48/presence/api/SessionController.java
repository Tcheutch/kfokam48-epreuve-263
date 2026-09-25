package com.kfokam48.presence.api;

import com.kfokam48.presence.api.dto.OuvrirSessionRequete;
import com.kfokam48.presence.api.dto.SessionCreeeReponse;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le contrôleur ne fait que traduire HTTP en appel de service et inversement :
 * aucune requête, aucune règle de gestion ici (contrainte B3).
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionCreeeReponse> ouvrir(@RequestBody(required = false) OuvrirSessionRequete requete) {
        OuvrirSessionRequete corps = requete == null ? new OuvrirSessionRequete(null, null) : requete;
        Session session = sessionService.ouvrir(corps.titre(), corps.promotionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionCreeeReponse.de(session));
    }
}
