package com.kfokam48.presence.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfokam48.presence.depot.ExerciceRepository;
import com.kfokam48.presence.depot.PresenceRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * RG11 — l'écran relecteur : {@code GET /api/etudiants/{id}/relectures}.
 *
 * <p><strong>Pas de {@code @Transactional} sur cette classe, et c'est tout
 * l'intérêt du test.</strong> L'annotation garderait la session Hibernate
 * ouverte pendant toute la méthode, donc les associations paresseuses
 * s'initialiseraient sans bruit et le défaut serait invisible — exactement
 * comme il l'a été pour le bug #22.
 *
 * <p>En production, {@code open-in-view} vaut {@code false} : la session est
 * fermée avant que le contrôleur ne convertisse en DTO. Toute association
 * encore paresseuse à ce moment-là lève une
 * {@link org.hibernate.LazyInitializationException}, que le filet de sécurité
 * transforme en {@code 500}. Ce test reproduit cette condition.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EtudiantControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private SessionRepository sessions;
    @Autowired private ExerciceRepository exercices;
    @Autowired private PresenceRepository presences;
    @Autowired private RelectureRepository relectures;

    private Long relecteurId;

    @BeforeEach
    void preparer() {
        nettoyer();
        Promotion promotion = promotions.save(new Promotion("Promotion écran relecteur"));
        Utilisateur auteur = utilisateurs.save(new Utilisateur("Awa Ndiaye", Role.ETUDIANT, promotion));
        Utilisateur relecteur = utilisateurs.save(new Utilisateur("Bilal Moussa", Role.ETUDIANT, promotion));
        relecteurId = relecteur.getId();

        Session session = sessions.save(new Session("Spring Boot — jour 4", "ECRAN1", Instant.now(), promotion, null));
        Exercice exercice =
                exercices.save(new Exercice(session, auteur, "https://github.com/awa/tp4", Instant.now()));
        exercice.marquerEnAttente();
        exercices.save(exercice);
        relectures.save(new Relecture(exercice, relecteur, Instant.now()));
    }

    @AfterEach
    void nettoyer() {
        relectures.deleteAll();
        exercices.deleteAll();
        presences.deleteAll();
        sessions.deleteAll();
        utilisateurs.deleteAll();
        promotions.deleteAll();
    }

    @Test
    @DisplayName("RG11 — l'écran relecteur répond 200, hors transaction, avec toutes ses données")
    void lEcranRelecteurRepondHorsTransaction() throws Exception {
        mockMvc.perform(get("/api/etudiants/" + relecteurId + "/relectures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value("ASSIGNEE"))
                // Ces trois champs traversent des associations paresseuses :
                // relecture -> exercice -> session, et exercice -> etudiant.
                // Ce sont eux qui échouaient en dehors de la transaction.
                .andExpect(jsonPath("$[0].sessionTitre").value("Spring Boot — jour 4"))
                .andExpect(jsonPath("$[0].lien").value("https://github.com/awa/tp4"))
                .andExpect(jsonPath("$[0].auteurNom").value("Awa Ndiaye"))
                .andExpect(jsonPath("$[0].modifiable").value(true));
    }

    @Test
    @DisplayName("Un étudiant sans relecture assignée reçoit une liste vide, pas une erreur")
    void aucuneRelectureRenvoieUneListeVide() throws Exception {
        mockMvc.perform(get("/api/etudiants/999999/relectures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
