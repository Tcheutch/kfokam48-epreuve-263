package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kfokam48.presence.depot.PromotionRepository;
import com.kfokam48.presence.depot.SessionRepository;
import com.kfokam48.presence.depot.UtilisateurRepository;
import com.kfokam48.presence.domaine.Promotion;
import com.kfokam48.presence.domaine.Role;
import com.kfokam48.presence.domaine.Session;
import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

/**
 * Tests unitaires de l'ouverture de session — EF1.
 *
 * <p>Les noms de méthodes citent la règle de gestion couverte : c'est ce qui
 * rend le lien entre le cahier des charges et le code vérifiable par un tiers.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    private static final Instant MIDI = Instant.parse("2026-09-25T12:00:00Z");

    @Mock private SessionRepository sessions;
    @Mock private PromotionRepository promotions;
    @Mock private UtilisateurRepository utilisateurs;
    @Mock private GenerateurCode generateurCode;

    private SessionService service;
    private Promotion promotion;

    @BeforeEach
    void preparer() {
        Clock horlogeFigee = Clock.fixed(MIDI, ZoneOffset.UTC);
        service = new SessionService(sessions, promotions, utilisateurs, generateurCode, horlogeFigee);

        promotion = new Promotion("Promotion Java 2026");
        when(promotions.findById(1L)).thenReturn(Optional.of(promotion));
        when(utilisateurs.findFirstByPromotionIdAndRole(anyLong(), any(Role.class))).thenReturn(Optional.empty());
        when(generateurCode.genere()).thenReturn("K7M2QX");
        when(sessions.existsByCode(anyString())).thenReturn(false);
        when(sessions.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("RG2 — le code expire exactement quinze minutes après l'ouverture")
    void rg2_expirationEstOuverturePlusQuinzeMinutes() {
        Session session = service.ouvrir("Spring Boot — jour 4", 1L);

        assertThat(session.getOuvertureAt()).isEqualTo(MIDI);
        assertThat(session.getExpirationAt()).isEqualTo(MIDI.plus(Duration.ofMinutes(15)));
        assertThat(Duration.between(session.getOuvertureAt(), session.getExpirationAt()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("RG2 — la session est ouverte avant l'expiration, expirée à la minute pile")
    void rg2_leCodeExpireDesLaQuinziemeMinute() {
        Session session = service.ouvrir("Spring Boot — jour 4", 1L);

        assertThat(session.codeExpireA(MIDI.plus(Duration.ofMinutes(14)))).isFalse();
        assertThat(session.codeExpireA(MIDI.plus(Duration.ofSeconds(899)))).isFalse();
        // À la quinzième minute pile, le code ne marche plus : « 15 minutes après,
        // il ne marche plus » (Q2) se lit comme une borne exclue.
        assertThat(session.codeExpireA(MIDI.plus(Duration.ofMinutes(15)))).isTrue();
        assertThat(session.codeExpireA(MIDI.plus(Duration.ofMinutes(16)))).isTrue();
    }

    @Test
    @DisplayName("La session ouvre à l'état OUVERTE, sans date de clôture")
    void uneSessionNaitOuverteEtNonCloturee() {
        Session session = service.ouvrir("Spring Boot — jour 4", 1L);

        assertThat(session.estCloturee()).isFalse();
        assertThat(session.getClotureAt()).isNull();
        assertThat(session.getCode()).isEqualTo("K7M2QX");
    }

    @Test
    @DisplayName("Un titre vide ou absent est refusé par CHAMP_MANQUANT")
    void titreVideEstRefuse() {
        assertThatThrownBy(() -> service.ouvrir("   ", 1L))
                .isInstanceOf(ErreurMetier.class)
                .extracting(e -> ((ErreurMetier) e).code())
                .isEqualTo(CodeErreur.CHAMP_MANQUANT);

        assertThatThrownBy(() -> service.ouvrir(null, 1L)).isInstanceOf(ErreurMetier.class);
        assertThatThrownBy(() -> service.ouvrir("Titre", null)).isInstanceOf(ErreurMetier.class);
    }

    @Test
    @DisplayName("Une promotion inconnue sort en 400 : le contrat imposé n'autorise pas 404 ici")
    void promotionInconnueSortEn400EtNonEn404() {
        when(promotions.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ouvrir("Spring Boot", 999L))
                .isInstanceOf(ErreurMetier.class)
                .satisfies(e -> {
                    ErreurMetier erreur = (ErreurMetier) e;
                    assertThat(erreur.code()).isEqualTo(CodeErreur.PROMOTION_INCONNUE);
                    assertThat(erreur.statut()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("RG21 — un code déjà pris est retiré jusqu'à en trouver un libre")
    void rg21_leCodeEstUnique() {
        when(generateurCode.genere()).thenReturn("PRIS01", "PRIS02", "LIBRE3");
        when(sessions.existsByCode("PRIS01")).thenReturn(true);
        when(sessions.existsByCode("PRIS02")).thenReturn(true);
        when(sessions.existsByCode("LIBRE3")).thenReturn(false);

        Session session = service.ouvrir("Spring Boot", 1L);

        assertThat(session.getCode()).isEqualTo("LIBRE3");
    }
}
