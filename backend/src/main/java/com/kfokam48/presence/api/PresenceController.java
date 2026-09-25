package com.kfokam48.presence.api;

import com.kfokam48.presence.api.dto.MarquerPresenceRequete;
import com.kfokam48.presence.api.dto.PresenceReponse;
import com.kfokam48.presence.domaine.Presence;
import com.kfokam48.presence.service.PresenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping
    public ResponseEntity<PresenceReponse> marquer(@RequestBody(required = false) MarquerPresenceRequete requete) {
        MarquerPresenceRequete corps = requete == null ? new MarquerPresenceRequete(null, null) : requete;
        Presence presence = presenceService.marquerAvecCode(corps.code(), corps.etudiantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PresenceReponse.de(presence));
    }
}
