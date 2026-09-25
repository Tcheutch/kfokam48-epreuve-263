package com.kfokam48.presence.service;

import com.kfokam48.presence.erreur.CodeErreur;
import com.kfokam48.presence.erreur.ErreurMetier;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Component;

/**
 * RG18 (hypothèse H5) — le contrat impose {@code 400 LIEN_INVALIDE} sans dire
 * ce qu'est un lien valide, et {@code format: uri} n'est pas contraignant.
 *
 * <p>La définition retenue : URL absolue, schéma {@code http} ou {@code https},
 * hôte présent, 2 048 caractères au plus. <strong>Aucun appel réseau</strong>
 * n'est fait : ni l'accessibilité ni le domaine ne sont vérifiés. Un lien mort
 * est donc accepté — le relecteur le signalera dans son commentaire. Vérifier
 * l'accessibilité ferait dépendre un dépôt d'exercice de la disponibilité d'un
 * site tiers, ce qui est pire que le mal.
 */
@Component
public class ValidateurLien {

    public static final int LONGUEUR_MAX = 2048;

    public String valider(String lien) {
        if (lien == null || lien.isBlank()) {
            throw new ErreurMetier(CodeErreur.CHAMP_MANQUANT);
        }

        String normalise = lien.trim();
        if (normalise.length() > LONGUEUR_MAX) {
            throw new ErreurMetier(CodeErreur.LIEN_INVALIDE);
        }

        URI uri;
        try {
            uri = new URI(normalise);
        } catch (URISyntaxException e) {
            throw new ErreurMetier(CodeErreur.LIEN_INVALIDE);
        }

        if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ErreurMetier(CodeErreur.LIEN_INVALIDE);
        }

        String schema = uri.getScheme().toLowerCase();
        if (!schema.equals("http") && !schema.equals("https")) {
            throw new ErreurMetier(CodeErreur.LIEN_INVALIDE);
        }

        return normalise;
    }
}
