package com.kfokam48.presence.api.dto;

import java.math.BigDecimal;

/**
 * Corps de {@code POST /api/relectures/{id}}.
 *
 * <p>{@code note} et {@code commentaire} sont imposés. {@code relecteurId} est
 * un ajout <strong>facultatif</strong>, et il répond à un trou du contrat
 * (hypothèse H12) : le corps imposé ne dit pas <em>qui</em> agit, alors que le
 * contrat exige un {@code 403 AUTO_RELECTURE}. Sans identité de l'appelant,
 * cette erreur serait inatteignable. Le champ reste facultatif, donc l'ensemble
 * {@code required [note, commentaire]} du contrat est inchangé.
 *
 * <p>{@code note} est reçue en {@link BigDecimal} et non en {@code int} : une
 * note décimale doit produire {@code 400 NOTE_INVALIDE} (RG9), pas une erreur
 * de désérialisation qui dirait « champ manquant ».
 */
public record RendreRelectureRequete(BigDecimal note, String commentaire, Long relecteurId) {}
