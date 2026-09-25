package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** RG18 / H5 — ce qu'est un lien valide, puisque le client ne l'a jamais dit. */
class ValidateurLienTest {

    private final ValidateurLien validateur = new ValidateurLien();

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://github.com/exemple/tp4",
                "http://exemple.cm/tp",
                "https://gitlab.com/awa/tp4/-/blob/main/README.md",
                "HTTPS://EXEMPLE.COM/TP"
            })
    @DisplayName("RG18 — une URL absolue en http ou https est acceptée")
    void rg18_liensAcceptes(String lien) {
        assertThat(validateur.valider(lien)).isEqualTo(lien.trim());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "exemple.com/tp",
                "/chemin/relatif",
                "ftp://exemple.com/tp",
                "javascript:alert(1)",
                "file:///etc/passwd",
                "https://",
                "pas une url du tout"
            })
    @DisplayName("RG18 — tout le reste est refusé par LIEN_INVALIDE")
    void rg18_liensRefuses(String lien) {
        assertThatThrownBy(() -> validateur.valider(lien))
                .isInstanceOf(ErreurMetier.class)
                .extracting(e -> ((ErreurMetier) e).code())
                .isEqualTo(CodeErreur.LIEN_INVALIDE);
    }

    @Test
    @DisplayName("H5 — au-delà de 2048 caractères, le lien est refusé")
    void h5_lienTropLongRefuse() {
        String tropLong = "https://exemple.com/" + "a".repeat(ValidateurLien.LONGUEUR_MAX);

        assertThatThrownBy(() -> validateur.valider(tropLong))
                .isInstanceOf(ErreurMetier.class)
                .extracting(e -> ((ErreurMetier) e).code())
                .isEqualTo(CodeErreur.LIEN_INVALIDE);
    }

    @Test
    @DisplayName("Un lien absent est un champ manquant, pas un lien invalide")
    void lienAbsentEstUnChampManquant() {
        assertThatThrownBy(() -> validateur.valider(null))
                .isInstanceOf(ErreurMetier.class)
                .extracting(e -> ((ErreurMetier) e).code())
                .isEqualTo(CodeErreur.CHAMP_MANQUANT);
    }
}
