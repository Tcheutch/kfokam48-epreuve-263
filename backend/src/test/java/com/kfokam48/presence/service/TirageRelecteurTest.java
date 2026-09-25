package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam48.presence.depot.PresenceRepository;
import com.kfokam48.presence.depot.RelectureRepository;
import com.kfokam48.presence.domaine.Exercice;
import com.kfokam48.presence.domaine.Presence;
import com.kfokam48.presence.domaine.Promotion;
import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.domaine.SourcePresence;
import com.kfokam48.presence.domaine.StatutExercice;
import com.kfokam48.presence.domaine.Utilisateur;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * EF4 — le tirage du relecteur.
 *
 * <p>Le hasard est injecté avec une graine fixe : un test qui ne passe que la
 * plupart du temps ne prouve rien.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TirageRelecteurTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T10:00:00Z");

    @Mock private PresenceRepository presences;
    @Mock private RelectureRepository relectures;

    private Promotion promotion;
    private Session session;
    private Utilisateur auteur;
    private TirageRelecteur tirage;

    @BeforeEach
    void preparer() {
        promotion = entite(new Promotion("Promotion Java"), 1L);
        session = entite(new Session("Séance", "ABC234", MAINTENANT, promotion, null), 100L);
        auteur = etudiant(10L);

        when(relectures.findByExerciceId(anyLong())).thenReturn(Optional.empty());
        when(relectures.save(any(Relecture.class))).thenAnswer(i -> i.getArgument(0));

        tirage = new TirageRelecteur(presences, relectures, new Random(1234));
    }

    @Test
    @DisplayName("RG5 + RG7 — l'auteur n'est jamais tiré, même seul avec un autre présent")
    void rg5_lAuteurNestJamaisTire() {
        Utilisateur autre = etudiant(11L);
        presentsSontPresents(auteur, autre);
        Exercice exercice = exerciceDe(auteur);

        Optional<Relecture> relecture = tirage.assigner(exercice, MAINTENANT);

        assertThat(relecture).isPresent();
        assertThat(relecture.get().getRelecteur().getId()).isEqualTo(11L);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE);
    }

    @Test
    @DisplayName("RG7 — sur cent tirages, tous les présents sortent, et jamais l'auteur")
    void rg7_leTirageParcourtTousLesPresents() {
        List<Utilisateur> presents = new ArrayList<>(List.of(auteur, etudiant(11L), etudiant(12L), etudiant(13L)));
        presentsSontPresents(presents.toArray(new Utilisateur[0]));

        Set<Long> relecteursTires = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Exercice exercice = exerciceDe(auteur);
            tirage.assigner(exercice, MAINTENANT).ifPresent(r -> relecteursTires.add(r.getRelecteur().getId()));
        }

        assertThat(relecteursTires).containsExactlyInAnyOrder(11L, 12L, 13L);
        assertThat(relecteursTires).doesNotContain(10L);
    }

    @Test
    @DisplayName("RG22 — auteur seul présent : aucun relecteur, l'exercice reste DEPOSE")
    void rg22_auteurSeulPresentLaisseLExerciceDepose() {
        presentsSontPresents(auteur);
        Exercice exercice = exerciceDe(auteur);

        Optional<Relecture> relecture = tirage.assigner(exercice, MAINTENANT);

        assertThat(relecture).isEmpty();
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.DEPOSE);
        verify(relectures, never()).save(any());
    }

    @Test
    @DisplayName("RG22 — personne de présent du tout : même comportement, sans erreur")
    void rg22_aucunPresentNEstPasUneErreur() {
        presentsSontPresents();
        Exercice exercice = exerciceDe(auteur);

        assertThat(tirage.assigner(exercice, MAINTENANT)).isEmpty();
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.DEPOSE);
    }

    @Test
    @DisplayName("RG6 — un exercice déjà relu ne se voit pas assigner un second relecteur")
    void rg6_unSeulRelecteurParExercice() {
        presentsSontPresents(auteur, etudiant(11L));
        Exercice exercice = exerciceDe(auteur);
        when(relectures.findByExerciceId(exercice.getId()))
                .thenReturn(Optional.of(new Relecture(exercice, etudiant(12L), MAINTENANT)));

        assertThat(tirage.assigner(exercice, MAINTENANT)).isEmpty();
        verify(relectures, never()).save(any());
    }

    @Test
    @DisplayName("RG7 — un formateur présent n'est pas un relecteur éligible")
    void rg7_leFormateurNestPasEligible() {
        Utilisateur formateur = entite(new Utilisateur("Madame Ngo Bell", Role.FORMATEUR, promotion), 1L);
        presentsSontPresents(auteur, formateur);
        Exercice exercice = exerciceDe(auteur);

        assertThat(tirage.assigner(exercice, MAINTENANT)).isEmpty();
    }

    // --- fabriques ---

    private void presentsSontPresents(Utilisateur... utilisateurs) {
        List<Presence> liste = new ArrayList<>();
        long id = 1;
        for (Utilisateur utilisateur : utilisateurs) {
            liste.add(entite(
                    new Presence(session, utilisateur, SourcePresence.ETUDIANT, MAINTENANT), id++));
        }
        when(presences.findBySessionId(100L)).thenReturn(liste);
    }

    private Exercice exerciceDe(Utilisateur etudiant) {
        return entite(new Exercice(session, etudiant, "https://exemple.com/tp", MAINTENANT), 500L);
    }

    private Utilisateur etudiant(Long id) {
        return entite(new Utilisateur("Étudiant " + id, Role.ETUDIANT, promotion), id);
    }

    private static <T> T entite(T entite, Long id) {
        try {
            var champ = entite.getClass().getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(entite, id);
            return entite;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
