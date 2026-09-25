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
import com.kfokam48.presence.domaine.Promotion;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * EF4 de bout en bout, et surtout RG22 — le cas que le client n'a pas prévu.
 *
 * <p>Ce test est celui qui prouve l'hypothèse H3 : un exercice déposé alors que
 * personne d'autre n'est présent ne reste pas orphelin pour toujours ; le
 * tirage est rejoué dès qu'un camarade marque sa présence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TirageRelecteurIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private SessionRepository sessions;
    @Autowired private ExerciceRepository exercices;
    @Autowired private RelectureRepository relectures;

    private Long auteurId;
    private Long camaradeId;
    private Long sessionId;

    @BeforeEach
    void preparer() {
        Promotion promotion = promotions.save(new Promotion("Promotion Java — test"));
        auteurId = utilisateurs.save(new Utilisateur("Awa Ndiaye", Role.ETUDIANT, promotion)).getId();
        camaradeId = utilisateurs.save(new Utilisateur("Bilal Moussa", Role.ETUDIANT, promotion)).getId();
        sessionId = sessions
                .save(new Session("Séance du jour", "TIRAGE", Instant.now(), promotion, null))
                .getId();
    }

    private void marquerPresence(Long etudiantId) throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TIRAGE\",\"etudiantId\":" + etudiantId + "}"))
                .andExpect(status().isCreated());
    }

    private Long deposer(Long etudiantId, String statutAttendu) throws Exception {
        String corps = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId
                                + ",\"lien\":\"https://github.com/tp4\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value(statutAttendu))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.valueOf(corps.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }

    @Test
    @DisplayName("EF4 — un camarade présent est tiré : l'exercice passe EN_ATTENTE")
    void unCamaradePresentEstTire() throws Exception {
        marquerPresence(auteurId);
        marquerPresence(camaradeId);

        Long exerciceId = deposer(auteurId, "EN_ATTENTE");

        var relecture = relectures.findByExerciceId(exerciceId).orElseThrow();
        assertThat(relecture.getRelecteur().getId()).isEqualTo(camaradeId);
        assertThat(relecture.getNote()).isNull();
    }

    @Test
    @DisplayName("RG22 — auteur seul présent : l'exercice reste DEPOSE, sans relecture")
    void auteurSeulPresentLaisseLExerciceDepose() throws Exception {
        marquerPresence(auteurId);

        Long exerciceId = deposer(auteurId, "DEPOSE");

        assertThat(relectures.findByExerciceId(exerciceId)).isEmpty();
    }

    @Test
    @DisplayName("RG22 — la présence d'un camarade, plus tard, rattrape l'exercice orphelin")
    void uneNouvellePresenceRejoueLeTirage() throws Exception {
        marquerPresence(auteurId);
        Long exerciceId = deposer(auteurId, "DEPOSE");
        assertThat(relectures.findByExerciceId(exerciceId)).isEmpty();

        // Le camarade arrive en retard — le tirage est rejoué pour lui.
        marquerPresence(camaradeId);

        var relecture = relectures.findByExerciceId(exerciceId).orElseThrow();
        assertThat(relecture.getRelecteur().getId()).isEqualTo(camaradeId);
        assertThat(exercices.findById(exerciceId).orElseThrow().getStatut())
                .isEqualTo(StatutExercice.EN_ATTENTE);
    }

    @Test
    @DisplayName("RG5 — un étudiant absent au moment du dépôt peut quand même déposer (H10)")
    void unEtudiantAbsentPeutDeposer() throws Exception {
        marquerPresence(camaradeId);

        // L'auteur n'a jamais marqué sa présence : le dépôt reste permis.
        Long exerciceId = deposer(auteurId, "EN_ATTENTE");

        assertThat(relectures.findByExerciceId(exerciceId).orElseThrow().getRelecteur().getId())
                .isEqualTo(camaradeId);
    }
}
