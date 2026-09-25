package com.kfokam48.presence.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** EF3 — dépôt et remplacement du lien, de bout en bout. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExerciceControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private SessionRepository sessions;
    @Autowired private ObjectMapper json;

    private Long etudiantId;
    private Long sessionOuverteId;
    private Long sessionExpireeId;
    private Long sessionClotureeId;

    @BeforeEach
    void preparer() {
        Promotion promotion = promotions.save(new Promotion("Promotion Java — test"));
        etudiantId = utilisateurs.save(new Utilisateur("Awa Ndiaye", Role.ETUDIANT, promotion)).getId();

        sessionOuverteId = sessions
                .save(new Session("Ouverte", "OUVERT", Instant.now(), promotion, null))
                .getId();

        // RG12 — expirée, mais le dépôt doit rester possible. C'est le point
        // que Q12 impose et qu'on rate facilement.
        sessionExpireeId = sessions
                .save(new Session("Expirée", "EXPIRE", Instant.now().minus(Duration.ofHours(4)), promotion, null))
                .getId();

        Session cloturee = new Session("Clôturée", "CLOTUR", Instant.now(), promotion, null);
        cloturee.cloturer(Instant.now());
        sessionClotureeId = sessions.save(cloturee).getId();
    }

    private ResultActions deposer(Long sessionId, String lien) throws Exception {
        return mockMvc.perform(post("/api/exercices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId + ",\"lien\":\"" + lien + "\"}"));
    }

    @Test
    @DisplayName("EF3 — 201 avec les deux champs imposés, id et statut")
    void depotRenvoie201() throws Exception {
        deposer(sessionOuverteId, "https://github.com/awa/tp4")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("DEPOSE"));
    }

    @Test
    @DisplayName("RG12 — le dépôt reste possible après l'expiration du code")
    void rg12_depotPossibleSurSessionExpiree() throws Exception {
        deposer(sessionExpireeId, "https://github.com/awa/tp4").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("RG20 — mais plus du tout après la clôture : 409 SESSION_CLOTUREE")
    void rg20_depotImpossibleApresCloture() throws Exception {
        deposer(sessionClotureeId, "https://github.com/awa/tp4")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    @DisplayName("RG18 — un lien relatif : 400 LIEN_INVALIDE")
    void rg18_lienInvalideRenvoie400() throws Exception {
        deposer(sessionOuverteId, "exemple.com/tp")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    @DisplayName("RG16 — deuxième dépôt : 409 EXERCICE_DEJA_DEPOSE")
    void rg16_secondDepotRenvoie409() throws Exception {
        deposer(sessionOuverteId, "https://github.com/awa/tp4").andExpect(status().isCreated());

        deposer(sessionOuverteId, "https://github.com/awa/tp4-bis")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    @DisplayName("RG13 — le lien est remplaçable tant qu'aucune relecture n'est rendue")
    void rg13_remplacementDuLienAccepte() throws Exception {
        String reponse = deposer(sessionOuverteId, "https://github.com/awa/tp4")
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode exercice = json.readTree(reponse);

        mockMvc.perform(put("/api/exercices/" + exercice.get("id").asLong())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lien\":\"https://github.com/awa/tp4-corrige\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lien").value("https://github.com/awa/tp4-corrige"));
    }

    @Test
    @DisplayName("Un exercice inexistant : 404 EXERCICE_INCONNU au format imposé")
    void remplacementSurExerciceInconnuRenvoie404() throws Exception {
        mockMvc.perform(put("/api/exercices/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lien\":\"https://github.com/awa/tp4\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCICE_INCONNU"))
                .andExpect(jsonPath("$.message").isString());
    }
}
