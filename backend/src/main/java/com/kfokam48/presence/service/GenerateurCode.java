package com.kfokam48.presence.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * RG21 (hypothèse H6) — le code ne doit pas être devinable.
 *
 * <p>Q4 le dit en creux : « sinon ils vont deviner les codes entre eux ». Six
 * caractères tirés d'un {@link SecureRandom} dans un alphabet de 32 signes font
 * un peu plus d'un milliard de combinaisons ; c'est ce qui rend le blocage de
 * RG4 réellement dissuasif.
 *
 * <p>Les caractères ambigus à l'oral et à l'écrit sont exclus — {@code I}, {@code O},
 * {@code 0}, {@code 1} — parce que le code est lu à voix haute dans une salle.
 */
@Component
public class GenerateurCode {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGUEUR = 6;

    private final SecureRandom aleatoire = new SecureRandom();

    public String genere() {
        StringBuilder code = new StringBuilder(LONGUEUR);
        for (int i = 0; i < LONGUEUR; i++) {
            code.append(ALPHABET.charAt(aleatoire.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
