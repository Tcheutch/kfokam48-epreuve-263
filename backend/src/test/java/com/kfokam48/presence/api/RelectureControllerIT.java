package com.kfokam48.presence.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfokam48.presence.depot.ExerciceRepository;
import com.kfokam48.presence.depot.PromotionRepository;
import com.kfokam48.presence.depot.RelectureRepository;
import com.kfokam48.presence.depot.SessionRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.Promotion;
import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.domaine.StatutExercice;
import com.kfokam48.presence.domaine.Utilisateur;
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

/** EF5 — rendre une note et un commentaire, avec les quatre statuts du contrat. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelectureControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private SessionRepository sessions;
    @Autowired private ExerciceRepository exercices;
    @Autowired private RelectureRepository relectures;

    private Session session;
    private Utilisateur auteur;
    private Utilisateur relecteur;
    private Exercice exercice;
    private Long relectureId;

    @BeforeEach
    void preparer() {
        Promotion promotion = promotions.save(new Promotion("Promotion Java — test"));
        auteur = utilisateurs.save(new Utilisateur("Awa Ndiaye", Role.ETUDIANT, promotion));
        relecteur = utilisateurs.save(new Utilisateur("Bilal Moussa", Role.ETUDIANT, promotion));
        session = sessions.save(new Session("Séance", "RELECT", Instant.now(), promotion, null));

        exercice = exercices.save(new Exercice(session, auteur, "https://github.com/tp4", Instant.now()));
        exercice.marquerEnAttente();
        relectureId = relectures.save(new Relecture(exercice, relecteur, Instant.now())).getId();
    }

    private ResultActions rendre(String corps) throws Exception {
        return mockMvc.perform(post("/api/relectures/" + relectureId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps));
    }

    @Test
    @DisplayName("EF5 — note entière et commentaire : 200, et l'exercice passe RELU")
    void rendreRenvoie200() throws Exception {
        rendre("{\"note\":15,\"commentaire\":\"Bonne structure, tests manquants.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RENDUE"))
                .andExpect(jsonPath("$.note").value(15));

        assertThat(exercices.findById(exercice.getId()).orElseThrow().getStatut())
                .isEqualTo(StatutExercice.RELU);
    }

    @Test
    @DisplayName("RG9 — une note décimale : 400 NOTE_INVALIDE, et surtout pas un arrondi")
    void rg9_noteDecimaleRefusee() throws Exception {
        rendre("{\"note\":15.5,\"commentaire\":\"Correct.\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    @DisplayName("RG9 — les bornes : 0 et 20 passent, -1 et 21 sont refusées")
    void rg9_bornesDeLaNote() throws Exception {
        rendre("{\"note\":0,\"commentaire\":\"Rien rendu.\"}").andExpect(status().isOk());
        rendre("{\"note\":20,\"commentaire\":\"Parfait.\"}").andExpect(status().isOk());
        rendre("{\"note\":-1,\"commentaire\":\"?\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
        rendre("{\"note\":21,\"commentaire\":\"?\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    @DisplayName("RG5 — l'auteur qui se déclare relecteur : 403 AUTO_RELECTURE")
    void rg5_autoRelectureRefusee() throws Exception {
        rendre("{\"note\":20,\"commentaire\":\"Excellent travail.\",\"relecteurId\":" + auteur.getId() + "}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"));
    }

    @Test
    @DisplayName("H12 — un tiers qui n'est pas le relecteur assigné : 403")
    void h12_unTiersNePeutPasRendreLaRelecture() throws Exception {
        Utilisateur tiers = utilisateurs.save(
                new Utilisateur("Chantal Fotso", Role.ETUDIANT, session.getPromotion()));

        rendre("{\"note\":12,\"commentaire\":\"Bof.\",\"relecteurId\":" + tiers.getId() + "}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RELECTEUR_NON_ASSIGNE"));
    }

    @Test
    @DisplayName("RG10 — Q10 retenu contre Q15 : la note se corrige tant que la session est ouverte")
    void rg10_laNoteSeCorrigeAvantLaCloture() throws Exception {
        rendre("{\"note\":12,\"commentaire\":\"Première lecture.\"}").andExpect(status().isOk());

        rendre("{\"note\":16,\"commentaire\":\"Relu plus attentivement.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(16));

        assertThat(relectures.findById(relectureId).orElseThrow().getNote()).isEqualTo(16);
    }

    @Test
    @DisplayName("RG20 — après clôture, la correction est refusée : 409 RELECTURE_DEJA_RENDUE")
    void rg20_apresClotureLaNoteEstFigee() throws Exception {
        rendre("{\"note\":12,\"commentaire\":\"Première lecture.\"}").andExpect(status().isOk());

        session.cloturer(Instant.now());
        sessions.save(session);

        rendre("{\"note\":18,\"commentaire\":\"Trop tard.\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));
    }

    @Test
    @DisplayName("Une relecture inexistante : 404 RELECTURE_INCONNUE au format imposé")
    void relectureInconnueRenvoie404() throws Exception {
        mockMvc.perform(post("/api/relectures/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":15,\"commentaire\":\"?\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RELECTURE_INCONNUE"))
                .andExpect(jsonPath("$.message").isString());
    }
}
