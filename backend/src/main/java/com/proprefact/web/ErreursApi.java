package com.proprefact.web;

import com.proprefact.domain.IntrouvableException;
import com.proprefact.domain.TransitionInterditeException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduit les erreurs en réponses lisibles par le front-end (format « problem detail »). */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // doit passer avant le gestionnaire par défaut de Spring
class ErreursApi {

    /** Deux actions simultanées sur le même élément : la seconde est refusée plutôt qu'appliquée à l'aveugle. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail modificationConcurrente() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Cet élément vient d'être modifié par une autre action : rechargez la page");
    }

    /** Ajoute la propriété « erreurs » : un message par champ fautif. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail donneesInvalides(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        for (FieldError erreur : exception.getBindingResult().getFieldErrors()) {
            erreurs.putIfAbsent(erreur.getField(), erreur.getDefaultMessage());
        }
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Données invalides : vérifiez les champs signalés");
        probleme.setProperty("erreurs", erreurs);
        return probleme;
    }

    @ExceptionHandler(TransitionInterditeException.class)
    ProblemDetail transitionInterdite(TransitionInterditeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IntrouvableException.class)
    ProblemDetail introuvable(IntrouvableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** Filet de sécurité : une contrainte de la base refusée ne doit jamais donner une erreur 500. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail donneesEnConflit() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Enregistrement impossible : ces données sont en conflit avec des données existantes");
    }
}
