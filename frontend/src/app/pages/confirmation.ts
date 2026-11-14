import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../api';
import { messageErreur } from '../modeles';
import { Session } from '../session';

/** Page ouverte depuis le lien reçu par e-mail : active le compte puis connecte l'utilisateur. */
@Component({
  imports: [RouterLink],
  template: `
    <div class="carte" style="max-width: 640px; margin: 2rem auto">
      <h1>Confirmation de votre adresse</h1>
      @if (erreur()) {
        <p class="erreur" role="alert">{{ erreur() }}</p>
        <p class="info">
          Le lien a peut-être expiré ou déjà servi. Depuis la page de connexion, vous pouvez en
          demander un nouveau.
        </p>
        <a routerLink="/connexion">Aller à la connexion</a>
      } @else {
        <p class="info" role="status">Activation de votre compte…</p>
      }
    </div>
  `,
})
export class Confirmation {
  protected readonly erreur = signal('');

  constructor() {
    const session = inject(Session);
    const router = inject(Router);
    const jeton = inject(ActivatedRoute).snapshot.queryParamMap.get('jeton');
    if (!jeton) {
      this.erreur.set('Lien de confirmation incomplet.');
      return;
    }
    inject(Api)
      .confirmer(jeton)
      .subscribe({
        next: (j) => {
          session.ouvrir(j);
          router.navigateByUrl('/factures');
        },
        error: (e) => this.erreur.set(messageErreur(e)),
      });
  }
}
