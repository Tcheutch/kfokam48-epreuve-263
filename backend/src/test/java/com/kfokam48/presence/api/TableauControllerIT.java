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
import com.kfokam48.presence.domaine.Presence;
import com.kfokam48.presence.domaine.Promotion;
import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.domaine.SourcePresence;
import com.kfokam48.presence.domaine.Utilisateur;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF7 — le tableau du formateur.
 *
 * <p>Le jeu de données couvre les trois situations que Q16 demande de
 * distinguer : un étudiant noté, un étudiant qui a déposé mais n'a pas encore
 * été relu, et un étudiant qui n'a rien fait du tout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TableauControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PromotionRepository promotions;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private SessionRepository sessions;
    @Autowired private PresenceRepository presences;
    @Autowired private ExerciceRepository exercices;
    @Autowired private RelectureRepository relectures;

    private Long promotionId;

    @BeforeEach
    void preparer() {
        Promotion promotion = promotions.save(new Promotion("Promotion Java — test"));
        promotionId = promotion.getId();

        Utilisateur awa = utilisateurs.save(new Utilisateur("Awa Ndiaye", Role.ETUDIANT, promotion));
        Utilisateur bilal = utilisateurs.save(new Utilisateur("Bilal Moussa", Role.ETUDIANT, promotion));
        utilisateurs.save(new Utilisateur("Chantal Fotso", Role.ETUDIANT, promotion));
        // Un formateur dans la promotion : il ne doit PAS apparaître au tableau.
        utilisateurs.save(new Utilisateur("Madame Ngo Bell", Role.FORMATEUR, promotion));

        Session seance1 = sessions.save(new Session("Séance 1", "TAB001", Instant.now(), promotion, null));
        Session seance2 = sessions.save(new Session("Séance 2", "TAB002", Instant.now(), promotion, null));

        presences.save(new Presence(seance1, awa, SourcePresence.ETUDIANT, Instant.now()));
        presences.save(new Presence(seance2, awa, SourcePresence.FORMATEUR, Instant.now()));
        presences.save(new Presence(seance1, bilal, SourcePresence.ETUDIANT, Instant.now()));

        // Awa a déposé deux exercices, notés 14 et 17 -> moyenne 15,5.
        Exercice ex1 = exercices.save(new Exercice(seance1, awa, "https://exemple.com/1", Instant.now()));
        Exercice ex2 = exercices.save(new Exercice(seance2, awa, "https://exemple.com/2", Instant.now()));
        Relecture r1 = relectures.save(new Relecture(ex1, bilal, Instant.now()));
        Relecture r2 = relectures.save(new Relecture(ex2, bilal, Instant.now()));
        r1.rendre(14, "Correct.", Instant.now());
        r2.rendre(17, "Très bien.", Instant.now());
        relectures.save(r1);
        relectures.save(r2);

        // Bilal a déposé, et sa relecture lui reste à faire... non : c'est Awa
        // qui relit Bilal, et elle ne l'a pas rendue. Bilal reste sans note.
        Exercice ex3 = exercices.save(new Exercice(seance1, bilal, "https://exemple.com/3", Instant.now()));
        relectures.save(new Relecture(ex3, awa, Instant.now()));
    }

    @Test
    @DisplayName("EF7 — une ligne par étudiant, les six champs imposés, le formateur exclu")
    void tableauRenvoieUneLigneParEtudiant() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", promotionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nom").value("Awa Ndiaye"))
                .andExpect(jsonPath("$[0].etudiantId").isNumber())
                .andExpect(jsonPath("$[0].presences").value(2))
                .andExpect(jsonPath("$[0].exercicesDeposes").value(2))
                .andExpect(jsonPath("$[0].relecturesEnAttente").value(1));
    }

    @Test
    @DisplayName("RG19 — la moyenne des notes rendues : 14 et 17 font 15,5")
    void rg19_moyenneDesNotesRendues() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", promotionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moyenne").value(15.5));
    }

    @Test
    @DisplayName("RG19 — sans note reçue, la moyenne est null et surtout pas zéro")
    void rg19_sansNoteLaMoyenneEstNulle() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", promotionId.toString()))
                // Bilal a déposé, mais sa relecture n'est pas rendue.
                .andExpect(jsonPath("$[1].nom").value("Bilal Moussa"))
                .andExpect(jsonPath("$[1].exercicesDeposes").value(1))
                .andExpect(jsonPath("$[1].moyenne").doesNotExist())
                // Chantal n'a rien fait : elle apparaît quand même, à zéro.
                .andExpect(jsonPath("$[2].nom").value("Chantal Fotso"))
                .andExpect(jsonPath("$[2].presences").value(0))
                .andExpect(jsonPath("$[2].exercicesDeposes").value(0))
                .andExpect(jsonPath("$[2].moyenne").doesNotExist())
                .andExpect(jsonPath("$[2].relecturesEnAttente").value(0));
    }

    @Test
    @DisplayName("RG11 — relecturesEnAttente ne compte que les relectures assignées non rendues")
    void rg11_relecturesEnAttente() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", promotionId.toString()))
                // Awa doit relire Bilal et ne l'a pas fait.
                .andExpect(jsonPath("$[0].relecturesEnAttente").value(1))
                // Bilal a rendu ses deux relectures : il ne doit plus rien.
                .andExpect(jsonPath("$[1].relecturesEnAttente").value(0));
    }

    @Test
    @DisplayName("Promotion inconnue : 404 PROMOTION_INCONNUE au format imposé")
    void promotionInconnueRenvoie404() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("ENF4 — le paramètre promotionId manquant sort au format { code, message }")
    void parametreManquantSortAuFormatImpose() throws Exception {
        mockMvc.perform(get("/api/tableau"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.message").isString());
    }
}
