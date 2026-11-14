import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Api } from '../api/api';
import { Session } from '../auth/session';

/** Page ouverte depuis le lien reçu par e-mail : active le compte puis connecte l'utilisateur. */
@Component({
  imports: [RouterLink],
  template: `
    <div class="carte etroite">
      <h1>Confirmation de votre adresse</h1>
      @if (echec()) {
        <p class="info">
          Le lien est incomplet, a expiré ou a déjà servi. Depuis la page de connexion, vous pouvez
          en demander un nouveau.
        </p>
        <a routerLink="/connexion">Aller à la connexion</a>
      } @else {
        <p class="info" role="status">Activation de votre compte…</p>
      }
    </div>
  `,
})
export class Confirmation {
  /** Le détail de l'erreur est affiché par le composant racine ; ici, seulement la marche à suivre. */
  protected readonly echec = signal(false);

  constructor() {
    const session = inject(Session);
    const router = inject(Router);
    const jeton = inject(ActivatedRoute).snapshot.queryParamMap.get('jeton');
    if (!jeton) {
      this.echec.set(true);
      return;
    }
    inject(Api)
      .confirmer(jeton)
      .subscribe({
        next: (connexion) => {
          session.ouvrir(connexion);
          router.navigateByUrl('/factures');
        },
        error: () => this.echec.set(true),
      });
  }
}
