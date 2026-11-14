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
  /** Adresse à laquelle un lien de confirmation vient d'être envoyé. */
  protected readonly enAttente = signal('');
  protected readonly erreur = signal('');
  protected readonly info = signal('');
  protected readonly enCours = signal(false);
  protected readonly f = {
    email: '',
    motDePasse: '',
    raisonSociale: '',
    siren: '',
    tvaIntra: '',
    adresse: '',
    codePostal: '',
    ville: '',
    iban: '',
    bic: '',
    optionTvaDebits: false,
  };

  protected valider(): void {
    this.erreur.set('');
    this.info.set('');
    this.enCours.set(true);
    if (this.inscription()) {
      this.api.inscription(this.f).subscribe({
        next: () => {
          this.enAttente.set(this.f.email);
          this.f.motDePasse = '';
          this.inscription.set(false);
          this.enCours.set(false);
        },
        error: (e) => this.echec(e),
      });
    } else {
      this.api.connexion(this.f.email, this.f.motDePasse).subscribe({
        next: (jeton) => {
          this.session.ouvrir(jeton);
          this.router.navigateByUrl('/factures');
        },
        error: (e) => {
          // 403 : le mot de passe est bon mais l'adresse n'est pas confirmée.
          if (e?.status === 403) this.enAttente.set(this.f.email);
          this.echec(e);
        },
      });
    }
  }

  protected renvoyer(): void {
    this.erreur.set('');
    this.api.renvoyerConfirmation(this.enAttente()).subscribe({
      next: () =>
        this.info.set("Si un compte attend sa confirmation, un nouveau lien vient d'être envoyé."),
      error: (e) => this.echec(e),
    });
  }

  protected basculer(): void {
    this.inscription.set(!this.inscription());
    this.erreur.set('');
    this.info.set('');
    this.enAttente.set('');
  }

  private echec(e: unknown): void {
    this.erreur.set(messageErreur(e));
    this.enCours.set(false);
  }
}
