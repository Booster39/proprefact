package com.proprefact.web;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ErreursApi {

    /** Deux actions simultanées sur la même facture : la seconde est refusée plutôt qu'appliquée à l'aveugle. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail modificationConcurrente() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "La facture vient d'être modifiée par une autre action : rechargez la page");
    }
}
