package com.kfokam48.presence.api.dto;

/** Corps imposé de {@code POST /api/sessions} : {@code required [titre, promotionId]}. */
public record OuvrirSessionRequete(String titre, Long promotionId) {}
