import { Component, inject, input, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { Api } from '../api/api';

/** Rappel qu'un lien de confirmation a été envoyé, avec de quoi en demander un nouveau. */
@Component({
  selector: 'app-renvoi-confirmation',
  template: `
    <p class="info" role="status">
      Un e-mail de confirmation a été envoyé à <strong>{{ email() }}</strong>. Ouvrez le lien qu'il
      contient pour activer votre compte (valable 24 heures).
      <button type="button" class="secondaire petit" [disabled]="enCours()" (click)="renvoyer()">
        Renvoyer l'e-mail
      </button>
    </p>
    @if (renvoye()) {
      <p class="info" role="status">
        Si un compte attend sa confirmation, un nouveau lien vient d'être envoyé.
      </p>
    }
  `,
})
export class RenvoiConfirmation {
  private readonly api = inject(Api);

  readonly email = input.required<string>();
  protected readonly enCours = signal(false);
  protected readonly renvoye = signal(false);

  protected renvoyer(): void {
    if (this.enCours()) return;
    this.enCours.set(true);
    this.renvoye.set(false);
    this.api
      .renvoyerConfirmation(this.email())
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe(() => this.renvoye.set(true));
  }
}
