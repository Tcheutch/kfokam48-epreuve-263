package com.kfokam48.presence.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfokam48.presence.depot.PromotionRepository;
import com.kfokam48.presence.depot.SessionRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Promotion;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.domaine.Utilisateur;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF2 de bout en bout : les quatre issues de D3, avec les codes HTTP du contrat.
 *
 * <p>C'est le test qui prouve la conformité au contrat imposé — celui dont le
 * sujet dit qu'un écart vaut zéro sur le critère.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PresenceControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private SessionRepository sessions;

    private Long etudiantId;
    private Long etudiantAutrePromotionId;

    @BeforeEach
    void preparer() {
        Promotion promotion = promotions.save(new Promotion("Promotion Java — test"));
        Promotion autre = promotions.save(new Promotion("Promotion Web — test"));
        etudiantId = utilisateurs.save(new Utilisateur("Awa Ndiaye", Role.ETUDIANT, promotion)).getId();
        etudiantAutrePromotionId = utilisateurs.save(new Utilisateur("Grace Ateba", Role.ETUDIANT, autre)).getId();

        sessions.save(new Session("Séance ouverte", "OUVERT", Instant.now(), promotion, null));
        sessions.save(new Session(
                "Séance expirée", "EXPIRE", Instant.now().minus(Duration.ofMinutes(20)), promotion, null));
        Session cloturee =
                new Session("Séance clôturée", "CLOTUR", Instant.now(), promotion, null);
        cloturee.cloturer(Instant.now());
        sessions.save(cloturee);
    }

    private org.springframework.test.web.servlet.ResultActions marquer(String code, Long etudiant) throws Exception {
        return mockMvc.perform(post("/api/presences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiant + "}"));
    }

    @Test
    @DisplayName("Cas nominal — 201 avec les quatre champs imposés et source ETUDIANT")
    void nominalRenvoie201() throws Exception {
        marquer("OUVERT", etudiantId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.etudiantId").value(etudiantId))
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    @Test
    @DisplayName("RG17 — code inconnu : 400 CODE_INCONNU, et non 404")
    void codeInconnuRenvoie400() throws Exception {
        marquer("ZZZZZZ", etudiantId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("RG2 — code expiré : 410 CODE_EXPIRE")
    void codeExpireRenvoie410() throws Exception {
        marquer("EXPIRE", etudiantId)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
    }

    @Test
    @DisplayName("RG15 — deuxième marquage : 409 DEJA_PRESENT")
    void secondMarquageRenvoie409() throws Exception {
        marquer("OUVERT", etudiantId).andExpect(status().isCreated());

        marquer("OUVERT", etudiantId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    @Test
    @DisplayName("RG20 — session clôturée : 409 SESSION_CLOTUREE")
    void sessionClotureeRenvoie409() throws Exception {
        marquer("CLOTUR", etudiantId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    @DisplayName("H11 — étudiant d'une autre promotion : 400 ETUDIANT_HORS_PROMOTION")
    void etudiantHorsPromotionRenvoie400() throws Exception {
        marquer("OUVERT", etudiantAutrePromotionId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ETUDIANT_HORS_PROMOTION"));
    }

    @Test
    @DisplayName("Champ manquant — 400 CHAMP_MANQUANT")
    void champManquantRenvoie400() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"etudiantId\":" + etudiantId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }
}
