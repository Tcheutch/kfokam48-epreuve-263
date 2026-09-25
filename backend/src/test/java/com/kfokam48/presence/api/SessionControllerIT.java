package com.kfokam48.presence.api;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfokam48.presence.depot.PromotionRepository;
import com.kfokam48.presence.domaine.Promotion;
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
 * Test d'intégration de bout en bout sur {@code POST /api/sessions} (contrainte B6).
 *
 * <p>Il tourne sur H2 en mémoire, avec les migrations Flyway réellement livrées :
 * aucune base locale n'est nécessaire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;

    private Long promotionId;

    @BeforeEach
    void preparer() {
        promotionId = promotions.save(new Promotion("Promotion Java 2026 — test")).getId();
    }

    @Test
    @DisplayName("EF1 — 201 avec id, code, ouvertureAt et expirationAt")
    void ouvertureRenvoie201EtLeContratImpose() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Spring Boot — jour 4\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value(matchesPattern("[A-HJ-NP-Z2-9]{6}")))
                .andExpect(jsonPath("$.ouvertureAt").isString())
                .andExpect(jsonPath("$.expirationAt").isString());
    }

    @Test
    @DisplayName("EF1 — un titre vide renvoie 400 au format { code, message }")
    void titreVideRenvoie400AuFormatImpose() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"\",\"promotionId\":" + promotionId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("EF1 — une promotion inconnue renvoie 400 PROMOTION_INCONNUE, pas 404")
    void promotionInconnueRenvoie400() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Spring Boot\",\"promotionId\":999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    @DisplayName("ENF4 — un corps illisible sort au format { code, message }, sans trace d'exécution")
    void corpsIllisibleSortAuFormatImpose() throws Exception {
        mockMvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON).content("{ pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
