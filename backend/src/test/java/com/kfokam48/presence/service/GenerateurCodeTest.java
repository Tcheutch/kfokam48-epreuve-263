package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GenerateurCodeTest {

    private final GenerateurCode generateur = new GenerateurCode();

    @Test
    @DisplayName("RG21 — six caractères, sans les signes ambigus à l'oral (I, O, 0, 1)")
    void rg21_formeDuCode() {
        IntStream.range(0, 500)
                .mapToObj(i -> generateur.genere())
                .forEach(code -> assertThat(code).hasSize(6).matches("[A-HJ-NP-Z2-9]{6}"));
    }

    @Test
    @DisplayName("RG21 — le code n'est pas prévisible : mille tirages, pas de collision massive")
    void rg21_leCodeNestPasPrevisible() {
        Set<String> codes = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> codes.add(generateur.genere()));

        // Sur un espace de 32^6 ≈ 1,07 milliard, mille tirages sans quasi aucune
        // collision. On se garde une marge : le test ne doit pas devenir instable.
        assertThat(codes).hasSizeGreaterThan(995);
    }
}
