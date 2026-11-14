import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Session } from './session';

/** Réserve une route aux utilisateurs connectés ; les autres sont renvoyés à la connexion. */
export const connexionRequise: CanActivateFn = () =>
  inject(Session).connecte() || inject(Router).parseUrl('/connexion');
