package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.kfokam48.presence.depot.PresenceRepository;
import com.kfokam48.presence.depot.SessionRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Presence;
import com.kfokam48.presence.domaine.Promotion;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.domaine.SourcePresence;
import com.kfokam48.presence.domaine.Utilisateur;
import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * EF2 — les règles du marquage de présence, testées sans base ni HTTP.
 *
 * <p>Ce qui est vérifié ici et nulle part ailleurs : <strong>l'ordre</strong>
 * des contrôles. Quand plusieurs cas sont vrais en même temps — une session à
 * la fois clôturée et expirée, un étudiant à la fois hors promotion et déjà
 * présent — c'est l'ordre qui décide du code d'erreur renvoyé.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenceServiceTest {

    private static final Instant OUVERTURE = Instant.parse("2026-09-25T09:00:00Z");

    @Mock private PresenceRepository presences;
    @Mock private SessionRepository sessions;
    @Mock private UtilisateurRepository utilisateurs;

    private Promotion promotion;
    private Utilisateur etudiant;
    private Session session;

    private PresenceService serviceA(Instant maintenant) {
        promotion = promotionAvecId(1L);
        etudiant = utilisateurAvecId(10L, promotion);
        session = sessionAvecId(100L, "K7M2QX", OUVERTURE, promotion);

        when(utilisateurs.findById(10L)).thenReturn(Optional.of(etudiant));
        when(sessions.findByCode("K7M2QX")).thenReturn(Optional.of(session));
        when(presences.existsBySessionIdAndEtudiantId(anyLong(), anyLong())).thenReturn(false);
        when(presences.save(any(Presence.class))).thenAnswer(i -> i.getArgument(0));

        return new PresenceService(presences, sessions, utilisateurs, Clock.fixed(maintenant, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("RG14 — une présence marquée par l'étudiant porte source = ETUDIANT")
    void rg14_leMarquageParCodeProduitUneSourceEtudiant() {
        PresenceService service = serviceA(OUVERTURE.plus(Duration.ofMinutes(5)));

        Presence presence = service.marquerAvecCode("K7M2QX", 10L);

        assertThat(presence.getSource()).isEqualTo(SourcePresence.ETUDIANT);
        assertThat(presence.getEtudiant()).isSameAs(etudiant);
        assertThat(presence.getSession()).isSameAs(session);
    }

    @Test
    @DisplayName("RG2 — à la quinzième minute pile, le code est expiré : 410")
    void rg2_codeExpireApresQuinzeMinutes() {
        PresenceService serviceJusteAvant = serviceA(OUVERTURE.plus(Duration.ofSeconds(899)));
        assertThat(serviceJusteAvant.marquerAvecCode("K7M2QX", 10L)).isNotNull();

        PresenceService serviceApres = serviceA(OUVERTURE.plus(Duration.ofMinutes(15)));
        assertThatThrownBy(() -> serviceApres.marquerAvecCode("K7M2QX", 10L))
                .isInstanceOf(ErreurMetier.class)
                .extracting(e -> ((ErreurMetier) e).code())
                .isEqualTo(CodeErreur.CODE_EXPIRE);
    }

    @Test
    @DisplayName("RG17 — un code inconnu vaut 400 CODE_INCONNU, et surtout pas 404")
    void rg17_codeInconnuVaut400EtNon404() {
        PresenceService service = serviceA(OUVERTURE.plus(Duration.ofMinutes(1)));
        when(sessions.findByCode("ZZZZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marquerAvecCode("ZZZZZZ", 10L))
                .isInstanceOf(ErreurMetier.class)
                .satisfies(e -> {
                    ErreurMetier erreur = (ErreurMetier) e;
                    assertThat(erreur.code()).isEqualTo(CodeErreur.CODE_INCONNU);
                    assertThat(erreur.statut().value()).isEqualTo(400);
                });
    }

    @Test
    @DisplayName("RG15 — un second marquage du même couple renvoie DEJA_PRESENT")
    void rg15_secondMarquageRefuse() {
        PresenceService service = serviceA(OUVERTURE.plus(Duration.ofMinutes(1)));
        when(presences.existsBySessionIdAndEtudiantId(100L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.marquerAvecCode("K7M2QX", 10L))
                .isInstanceOf(ErreurMetier.class)
                .extracting(e -> ((ErreurMetier) e).code())
                .isEqualTo(CodeErreur.DEJA_PRESENT);
    }

    @Test
    @DisplayName("RG3 + RG20 — sur une session clôturée ET expirée, c'est la clôture qui parle")
    void rg20_laClotureLemporteSurLExpiration() {
        PresenceService service = serviceA(OUVERTURE.plus(Duration.ofHours(3)));
        session.cloturer(OUVERTURE.plus(Duration.ofHours(2)));

        assertThatThrownBy(() -> service.marquerAvecCode("K7M2QX", 10L))
                .isInstanceOf(ErreurMetier.class)
                .extracting(e -> ((ErreurMetier) e).code())
                // Et non CODE_EXPIRE : l'expiration est un délai, la clôture est
                // un état définitif. Dire « le code a expiré » laisserait croire
                // qu'une nouvelle session sauverait la mise.
                .isEqualTo(CodeErreur.SESSION_CLOTUREE);
    }

    @Test
    @DisplayName("H11 — un étudiant d'une autre promotion est refusé")
    void h11_etudiantHorsPromotionRefuse() {
        PresenceService service = serviceA(OUVERTURE.plus(Duration.ofMinutes(1)));
        Utilisateur intrus = utilisateurAvecId(99L, promotionAvecId(2L));
        when(utilisateurs.findById(99L)).thenReturn(Optional.of(intrus));

        assertThatThrownBy(() -> service.marquerAvecCode("K7M2QX", 99L))
                .isInstanceOf(ErreurMetier.class)
                .extracting(e -> ((ErreurMetier) e).code())
                .isEqualTo(CodeErreur.ETUDIANT_HORS_PROMOTION);
    }

    @Test
    @DisplayName("Le code est comparé sans tenir compte de la casse ni des espaces")
    void leCodeEstNormaliseAvantRecherche() {
        PresenceService service = serviceA(OUVERTURE.plus(Duration.ofMinutes(1)));

        assertThat(service.marquerAvecCode("  k7m2qx ", 10L)).isNotNull();
    }

    // --- fabriques : les identifiants sont posés par réflexion, puisque les
    // --- entités ne les exposent pas en écriture. C'est le prix d'un domaine
    // --- qui ne s'ouvre pas pour les tests.

    private static Promotion promotionAvecId(Long id) {
        Promotion promotion = new Promotion("Promotion " + id);
        poserId(promotion, id);
        return promotion;
    }

    private static Utilisateur utilisateurAvecId(Long id, Promotion promotion) {
        Utilisateur utilisateur = new Utilisateur("Étudiant " + id, Role.ETUDIANT, promotion);
        poserId(utilisateur, id);
        return utilisateur;
    }

    private static Session sessionAvecId(Long id, String code, Instant ouverture, Promotion promotion) {
        Session session = new Session("Séance", code, ouverture, promotion, null);
        poserId(session, id);
        return session;
    }

    private static void poserId(Object entite, Long id) {
        try {
            var champ = entite.getClass().getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(entite, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
