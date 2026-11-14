import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Api } from '../api/api';
import { Session } from '../auth/session';
import { RenvoiConfirmation } from './renvoi-confirmation';

@Component({
  imports: [ReactiveFormsModule, RouterLink, RenvoiConfirmation],
  template: `
    <div class="carte etroite">
      <h1>Connexion</h1>
      <form [formGroup]="form" (ngSubmit)="connecter()">
        <div class="grille">
          <label>E-mail <input type="email" formControlName="email" autocomplete="email" /></label>
          <label
            >Mot de passe
            <input type="password" formControlName="motDePasse" autocomplete="current-password"
          /></label>
        </div>
        @if (enAttente()) {
          <app-renvoi-confirmation [email]="enAttente()" />
        }
        <div class="actions">
          <button type="submit" [disabled]="form.invalid || enCours()">Se connecter</button>
          <a routerLink="/inscription">Créer un compte</a>
        </div>
      </form>
    </div>
  `,
})
export class Connexion {
  private readonly api = inject(Api);
  private readonly session = inject(Session);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  /** Adresse d'un compte dont le mot de passe est bon mais qui attend encore sa confirmation. */
  protected readonly enAttente = signal('');
  protected readonly enCours = signal(false);

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', Validators.required],
  });

  protected connecter(): void {
    if (this.form.invalid || this.enCours()) return;
    const { email, motDePasse } = this.form.getRawValue();
    this.enCours.set(true);
    this.enAttente.set('');
    this.api
      .connexion(email, motDePasse)
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe({
        next: (connexion) => {
          this.session.ouvrir(connexion);
          this.router.navigateByUrl('/factures');
        },
        // 403 : le mot de passe est bon mais l'adresse n'est pas confirmée.
        error: (e: HttpErrorResponse) => {
          if (e.status === 403) this.enAttente.set(email);
        },
      });
  }
}
