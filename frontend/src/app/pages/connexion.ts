import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Api } from '../api';
import { messageErreur } from '../modeles';
import { Session } from '../session';

@Component({
  imports: [FormsModule],
  templateUrl: './connexion.html',
})
export class Connexion {
  private readonly api = inject(Api);
  private readonly session = inject(Session);
  private readonly router = inject(Router);

  protected readonly inscription = signal(false);
  protected readonly erreur = signal('');
  protected readonly enCours = signal(false);
  protected readonly f = {
    email: '', motDePasse: '', raisonSociale: '', siren: '', tvaIntra: '',
    adresse: '', codePostal: '', ville: '', iban: '', bic: '', optionTvaDebits: false,
  };

  protected valider(): void {
    this.erreur.set('');
    this.enCours.set(true);
    const appel = this.inscription()
      ? this.api.inscription(this.f)
      : this.api.connexion(this.f.email, this.f.motDePasse);
    appel.subscribe({
      next: (jeton) => {
        this.session.ouvrir(jeton);
        this.router.navigateByUrl('/factures');
      },
      error: (e) => {
        this.erreur.set(messageErreur(e));
        this.enCours.set(false);
      },
    });
  }
}
