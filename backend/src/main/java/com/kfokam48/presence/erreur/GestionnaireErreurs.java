package com.kfokam48.presence.erreur;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Point de sortie unique des erreurs (contrainte B4, exigence ENF4).
 *
 * <p>Rien ne doit échapper à cette classe : ni une trace d'exécution, ni un corps
 * vide, ni la page d'erreur par défaut de Spring. Le dernier gestionnaire attrape
 * {@link Exception} pour cette raison.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreurs.class);

    /** Les erreurs prévues par le contrat. */
    @ExceptionHandler(ErreurMetier.class)
    public ResponseEntity<ReponseErreur> erreurMetier(ErreurMetier e) {
        return ResponseEntity.status(e.statut())
                .body(new ReponseErreur(e.code().name(), e.getMessage()));
    }

    /** Corps JSON absent, illisible, ou champ du mauvais type — une note décimale, par exemple. */
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ReponseErreur> requeteMalFormee(Exception e) {
        log.debug("Requête mal formée : {}", e.getMessage());
        return ResponseEntity.badRequest().body(ReponseErreur.de(CodeErreur.CHAMP_MANQUANT));
    }

    /** Une adresse inconnue est une erreur comme une autre : elle sort au même format. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ReponseErreur> adresseInconnue(NoResourceFoundException e, HttpServletRequest requete) {
        log.debug("Adresse inconnue : {}", requete.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ReponseErreur.de(CodeErreur.RESSOURCE_INCONNUE));
    }

    /** Le filet de sécurité. Sa présence est ce qui garantit ENF4. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ReponseErreur> erreurImprevue(Exception e, HttpServletRequest requete) {
        log.error("Erreur imprévue sur {} {}", requete.getMethod(), requete.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ReponseErreur.de(CodeErreur.ERREUR_INTERNE));
    }
}
