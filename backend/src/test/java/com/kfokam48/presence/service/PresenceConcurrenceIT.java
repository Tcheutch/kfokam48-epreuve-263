package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Issue #22 — « ils ont tapé le code presque en même temps et il n'y en a qu'un
 * seul qui apparaît dans ma liste ».
 *
 * <p><strong>Pas de {@code @Transactional} sur cette classe, volontairement.</strong>
 * Il enfermerait les deux appels dans une transaction unique et ferait
 * disparaître exactement le défaut qu'on cherche à démontrer. Les données sont
 * donc posées et nettoyées à la main.
 *
 * <p>Deux tests, pour deux défauts distincts :
 * <ol>
 *   <li>le <strong>couplage</strong> — l'échec du rejeu de RG22 annule une
 *       écriture qui n'a rien à voir avec lui. C'est le bug que le client
 *       décrit : « ma présence disparaît » ;</li>
 *   <li>la <strong>course</strong> sur {@code UNIQUE (exercice_id)} — deux
 *       tirages concurrents sur le même exercice. C'est ce qui déclenche le
 *       premier, mais ce n'en est pas la cause.</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PresenceConcurrenceIT.TirageQuiEchoue.class)
class PresenceConcurrenceIT {

    @Autowired private PresenceService presenceService;
    @Autowired private PromotionRepository promotions;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private SessionRepository sessions;
    @Autowired private ExerciceRepository exercices;
    @Autowired private PresenceRepository presences;
    @Autowired private RelectureRepository relectures;
    @Autowired private TirageQuiEchoue tirage;

    private Session session;
    private Utilisateur auteur;
    private Utilisateur bruno;
    private Utilisateur chantal;

    @BeforeEach
    void preparer() {
        nettoyer();
        tirage.reinitialiser();

        Promotion promotion = promotions.save(new Promotion("Promotion concurrence"));
        auteur = utilisateurs.save(new Utilisateur("Awa Ndiaye", Role.ETUDIANT, promotion));
        bruno = utilisateurs.save(new Utilisateur("Bruno Kamga", Role.ETUDIANT, promotion));
        chantal = utilisateurs.save(new Utilisateur("Chantal Fotso", Role.ETUDIANT, promotion));

        session = sessions.save(new Session("Séance du matin", "RACE01", Instant.now(), promotion, null));

        // La condition d'apparition : un exercice déposé alors que personne
        // d'autre n'était présent reste au statut DEPOSE (RG22). C'est lui que
        // les deux présences concurrentes vont tenter de rattraper.
        exercices.save(new Exercice(session, auteur, "https://exemple.com/tp4", Instant.now()));
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
    @DisplayName("#22 — l'échec du rejeu de RG22 ne doit pas emporter la présence")
    void laPresenceSurvitALEchecDuTirage() {
        tirage.echouerAuProchainAppel();

        try {
            presenceService.marquerAvecCode("RACE01", bruno.getId());
        } catch (RuntimeException attendu) {
            // Aujourd'hui l'exception remonte. Qu'elle remonte ou non n'est pas
            // le sujet : le sujet est ce qu'il reste en base derrière.
        }

        assertThat(presences.existsBySessionIdAndEtudiantId(session.getId(), bruno.getId()))
                .as("la présence de Bruno doit être enregistrée, quoi qu'il arrive au tirage : "
                        + "marquer sa présence et rattraper un exercice orphelin sont deux opérations "
                        + "sans rapport, elles ne doivent pas partager le sort l'une de l'autre")
                .isTrue();
    }

    @Test
    @DisplayName("#22 — deux présences simultanées sont toutes les deux enregistrées")
    void deuxPresencesSimultaneesSontToutesLesDeuxEnregistrees() throws Exception {
        // La barrière force l'entrelacement que les deux étudiants ont obtenu
        // par hasard : les deux transactions ont écrit leur présence et lu le
        // même exercice DEPOSE avant que l'une des deux n'insère sa relecture.
        tirage.synchroniserDeuxAppels();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> resultats = new ArrayList<>();
        for (Utilisateur etudiant : List.of(bruno, chantal)) {
            resultats.add(pool.submit(() -> {
                try {
                    presenceService.marquerAvecCode("RACE01", etudiant.getId());
                } catch (RuntimeException ignoree) {
                    // Le sujet du test est l'état final, pas l'exception.
                }
            }));
        }
        for (Future<?> resultat : resultats) {
            resultat.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(presences.findBySessionId(session.getId()))
                .as("les deux étudiants ont saisi un code valide : aucune des deux présences "
                        + "ne doit être perdue")
                .hasSize(2);

        assertThat(relectures.count())
                .as("RG6 n'est pas sacrifiée pour faire passer le test : l'exercice reçoit "
                        + "au plus une relecture")
                .isLessThanOrEqualTo(1);
    }

    /**
     * Décorateur de test du tirage : il sait échouer à la demande, et il sait
     * faire attendre deux appels l'un pour l'autre. Il ne change rien à la
     * logique du tirage, qu'il délègue.
     */
    @TestConfiguration
    static class TirageQuiEchoue extends TirageRelecteur {

        private volatile boolean echouerUneFois;
        private volatile CyclicBarrier barriere;

        TirageQuiEchoue(PresenceRepository presences, RelectureRepository relectures) {
            super(presences, relectures, new java.util.Random(7));
        }

        @Bean
        @Primary
        TirageRelecteur tirageDeTest() {
            return this;
        }

        void reinitialiser() {
            echouerUneFois = false;
            barriere = null;
        }

        void echouerAuProchainAppel() {
            echouerUneFois = true;
        }

        void synchroniserDeuxAppels() {
            barriere = new CyclicBarrier(2);
        }

        @Override
        public Optional<Relecture> assigner(Exercice exercice, Instant maintenant) {
            if (echouerUneFois) {
                echouerUneFois = false;
                throw new DataIntegrityViolationException(
                        "simulation d'une relecture déjà insérée par une transaction concurrente");
            }
            CyclicBarrier attente = barriere;
            if (attente != null) {
                try {
                    attente.await(20, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("barrière du test non franchie", e);
                }
            }
            return super.assigner(exercice, maintenant);
        }
    }
}
