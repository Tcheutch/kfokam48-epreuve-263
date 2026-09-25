package com.kfokam48.presence.api.dto;

/** Corps imposé de {@code POST /api/presences} : {@code required [code, etudiantId]}. */
public record MarquerPresenceRequete(String code, Long etudiantId) {}
