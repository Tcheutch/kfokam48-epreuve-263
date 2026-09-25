package com.kfokam48.presence.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * EF6 — la clôture, et ce qu'elle fige.
 *
 * <p>C'est le test qui donne sa valeur à l'hypothèse H1 : sans opération de
 * clôture, « tant que le formateur n'a pas clôturé » (Q10, Q12) ne voulait rien
 * dire d'exécutable. Ici, les quatre opérations que la clôture doit arrêter
 * sont vérifiées une par une.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClotureSessionIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private SessionRepository sessions;
    @Autowired private ExerciceRepository exercices;
    @Autowired private RelectureRepository relectures;

    private Long promotionId;
    private Long sessionId;
    private Long auteurId;
    private Long relecteurId;
    private Long exerciceId;
    private Long relectureId;

    @BeforeEach
    void preparer() {
        Promotion promotion = promotions.save(new Promotion("Promotion Java — test"));
        promotionId = promotion.getId();
        Utilisateur auteur = utilisateurs.save(new Utilisateur("Awa Ndiaye", Role.ETUDIANT, promotion));
        Utilisateur relecteur = utilisateurs.save(new Utilisateur("Bilal Moussa", Role.ETUDIANT, promotion));
        Utilisateur retardataire =
                utilisateurs.save(new Utilisateur("Chantal Fotso", Role.ETUDIANT, promotion));
        auteurId = auteur.getId();
        relecteurId = retardataire.getId();

        Session session = sessions.save(new Session("Séance", "CLOSE1", Instant.now(), promotion, null));
        sessionId = session.getId();

        Exercice exercice =
                exercices.save(new Exercice(session, auteur, "https://github.com/tp4", Instant.now()));
        exercice.marquerEnAttente();
        exerciceId = exercice.getId();
        relectureId = relectures.save(new Relecture(exercice, relecteur, Instant.now())).getId();
    }

    private void cloturer() throws Exception {
        mockMvc.perform(post("/api/sessions/" + sessionId + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etat").value("CLOTUREE"))
                .andExpect(jsonPath("$.clotureAt").isString());
    }

    @Test
    @DisplayName("EF6 — la clôture renvoie 200 et l'état CLOTUREE")
    void clotureRenvoie200() throws Exception {
        cloturer();
    }

    @Test
    @DisplayName("RG20 — la clôture est irréversible : la seconde renvoie 409")
    void rg20_secondeClotureRefusee() throws Exception {
        cloturer();

        mockMvc.perform(post("/api/sessions/" + sessionId + "/cloture"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    @DisplayName("RG20 — après la clôture, les quatre opérations sont refusées")
    void rg20_laClotureFigeToutesLesOperations() throws Exception {
        cloturer();

        // 1. marquer sa présence
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CLOSE1\",\"etudiantId\":" + relecteurId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));

        // 2. déposer un exercice
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + relecteurId
                                + ",\"lien\":\"https://github.com/tp5\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));

        // 3. remplacer le lien d'un exercice
        mockMvc.perform(put("/api/exercices/" + exerciceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lien\":\"https://github.com/tp4-corrige\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));

        // 4. rendre ou corriger une note
        mockMvc.perform(post("/api/relectures/" + relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":15,\"commentaire\":\"Trop tard.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    @DisplayName("Une session inexistante : 404 SESSION_INCONNUE")
    void sessionInconnueRenvoie404() throws Exception {
        mockMvc.perform(post("/api/sessions/999999/cloture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    @Test
    @DisplayName("EF12 — la liste des séances porte les trois états, jamais confondus")
    void ef12_listeDesSessionsAvecLeurEtat() throws Exception {
        cloturer();

        mockMvc.perform(get("/api/sessions").param("promotionId", promotionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].etat").value("CLOTUREE"))
                .andExpect(jsonPath("$[0].titre").value("Séance"));

        mockMvc.perform(get("/api/sessions").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    @DisplayName("L'auteur reste identifiable pour le formateur — la clôture ne perd rien")
    void lesDonneesRestentLisiblesApresCloture() throws Exception {
        cloturer();

        mockMvc.perform(get("/api/etudiants/" + auteurId + "/relectures")).andExpect(status().isOk());
    }
}
