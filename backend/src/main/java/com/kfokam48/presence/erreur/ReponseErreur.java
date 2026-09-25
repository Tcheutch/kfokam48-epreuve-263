package com.kfokam48.presence.erreur;

/** Le corps imposé pour TOUTES les erreurs, sans exception : {@code { code, message }}. */
public record ReponseErreur(String code, String message) {

    public static ReponseErreur de(CodeErreur code) {
        return new ReponseErreur(code.name(), code.message());
    }
}
