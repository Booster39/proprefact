import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Api } from '../api/api';
import { RenvoiConfirmation } from './renvoi-confirmation';

@Component({
  imports: [ReactiveFormsModule, RouterLink, RenvoiConfirmation],
  templateUrl: './inscription.html',
})
export class Inscription {
  private readonly api = inject(Api);
  private readonly fb = inject(NonNullableFormBuilder);

  /** Adresse à laquelle le lien de confirmation vient d'être envoyé ; vide tant que le compte n'est pas créé. */
  protected readonly envoyeA = signal('');
  protected readonly enCours = signal(false);

  // Les tailles sont celles que le back-end accepte (InscriptionCommande).
  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    motDePasse: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(72)]],
    raisonSociale: ['', [Validators.required, Validators.maxLength(200)]],
    siren: ['', [Validators.required, Validators.pattern(/^[0-9]{9}$/)]],
    tvaIntra: ['', Validators.maxLength(20)],
    adresse: ['', [Validators.required, Validators.maxLength(200)]],
    codePostal: ['', [Validators.required, Validators.maxLength(10)]],
    ville: ['', [Validators.required, Validators.maxLength(100)]],
    iban: ['', Validators.maxLength(34)],
    bic: ['', Validators.maxLength(11)],
    optionTvaDebits: [false],
  });

  protected inscrire(): void {
    if (this.form.invalid || this.enCours()) return;
    const donnees = this.form.getRawValue();
    this.enCours.set(true);
    this.api
      .inscription(donnees)
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe(() => {
        this.envoyeA.set(donnees.email);
        this.form.reset(); // le mot de passe ne reste pas dans la page
      });
  }
}
