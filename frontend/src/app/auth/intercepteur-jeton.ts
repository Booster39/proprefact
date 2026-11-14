import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { EMPTY, catchError, throwError } from 'rxjs';
import { ERREUR_TRANSMISE, messageErreur } from '../api/erreurs';
import { Notifications } from '../commun/notifications';
import { Session } from './session';

/**
 * Ajoute le jeton aux appels, renvoie à la connexion s'il a expiré, et affiche toute erreur du back-end.
 * Les écrans n'ont donc pas à gérer les erreurs : en cas d'échec, leur requête se termine sans résultat.
 * Un écran qui a besoin de l'erreur la demande avec ERREUR_TRANSMISE.
 */
export const intercepteurJeton: HttpInterceptorFn = (req, next) => {
  const session = inject(Session);
  const router = inject(Router);
  const notifications = inject(Notifications);
  const jeton = session.jeton();
  const requete = jeton ? req.clone({ setHeaders: { Authorization: `Bearer ${jeton}` } }) : req;
  // Une nouvelle action de l'utilisateur fait disparaître l'erreur de la précédente.
  if (req.method !== 'GET') {
    notifications.effacer();
  }
  return next(requete).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && !req.url.includes('/api/auth/')) {
        session.fermer();
        router.navigateByUrl('/connexion');
      } else {
        notifications.signaler(messageErreur(e));
      }
      return req.context.get(ERREUR_TRANSMISE) ? throwError(() => e) : EMPTY;
    }),
  );
};
