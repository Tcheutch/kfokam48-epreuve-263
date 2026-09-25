package com.kfokam48.presence.erreur;

import org.springframework.http.HttpStatus;

/** Toute erreur prévue par le contrat. Jamais d'exception technique remontée telle quelle. */
public class ErreurMetier extends RuntimeException {

    private final CodeErreur code;
    private final HttpStatus statut;

    public ErreurMetier(CodeErreur code) {
        this(code, code.statutHabituel(), code.message());
    }

    /** Pour les cas où le contrat imposé n'autorise pas le statut habituel du code. */
    public ErreurMetier(CodeErreur code, HttpStatus statut) {
        this(code, statut, code.message());
    }

    public ErreurMetier(CodeErreur code, HttpStatus statut, String message) {
        super(message);
        this.code = code;
        this.statut = statut;
    }

    public CodeErreur code() {
        return code;
    }

    public HttpStatus statut() {
        return statut;
    }
}
