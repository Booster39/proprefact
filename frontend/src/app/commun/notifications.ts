import { Injectable, signal } from '@angular/core';

/** Message d'erreur affiché en haut de la page par le composant racine, quel que soit l'écran. */
@Injectable({ providedIn: 'root' })
export class Notifications {
  readonly erreur = signal('');

  signaler(message: string): void {
    this.erreur.set(message);
  }

  effacer(): void {
    this.erreur.set('');
  }
}
