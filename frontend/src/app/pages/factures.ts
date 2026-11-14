import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Api } from '../api/api';
import {
  ACTIONS_PAR_STATUT,
  ActionFacture,
  CLASSE_STATUT,
  Client,
  Facture,
  LIBELLES_STATUT,
} from '../api/modeles';
import { DateFrPipe } from '../commun/date-fr.pipe';
import { aujourdhui } from '../commun/formats';
import { MontantPipe } from '../commun/montant.pipe';

/** Question posée avant une action qu'on ne peut pas annuler. */
const CONFIRMATIONS: Partial<Record<ActionFacture, string>> = {
  valider:
    'Valider cette facture ? Elle recevra un numéro définitif et ne pourra plus être modifiée ni supprimée.',
  payee: 'Marquer cette facture comme payée ?',
};

@Component({
  imports: [ReactiveFormsModule, MontantPipe, DateFrPipe],
  templateUrl: './factures.html',
})
export class Factures {
  private readonly api = inject(Api);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly libelles = LIBELLES_STATUT;
  protected readonly classes = CLASSE_STATUT;
  protected readonly actions = ACTIONS_PAR_STATUT;

  protected readonly factures = signal<Facture[]>([]);
  protected readonly clients = signal<Client[]>([]);
  protected readonly message = signal('');
  protected readonly ponctuelle = signal(false);
  protected readonly enCours = signal(false);

  protected readonly mois = this.fb.control(aujourdhui().slice(0, 7), Validators.required);

  protected readonly formPonctuelle = this.fb.group({
    clientId: this.fb.control<number | null>(null, Validators.required),
    designation: ['', [Validators.required, Validators.maxLength(500)]],
    quantite: [1, [Validators.required, Validators.min(0.001)]],
    prixUnitaireHt: [0, [Validators.required, Validators.min(0)]],
    tauxTva: [20, [Validators.required, Validators.min(0), Validators.max(100)]],
  });

  constructor() {
    this.recharger();
    this.api.clients().subscribe((clients) => this.clients.set(clients));
  }

  protected generer(): void {
    if (this.mois.invalid || !this.commencer()) return;
    this.api
      .genererMensuelles(this.mois.value)
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe((creees) => {
        this.message.set(
          creees.length === 0
            ? 'Rien à facturer : tous les contrats actifs ce mois-là sont déjà facturés.'
            : `${creees.length} brouillon(s) créé(s). Vérifiez-les puis validez-les.`,
        );
        this.recharger();
      });
  }

  protected creerPonctuelle(): void {
    const { clientId, ...ligne } = this.formPonctuelle.getRawValue();
    if (this.formPonctuelle.invalid || clientId === null || !this.commencer()) return;
    this.api
      .creerFacture(clientId, [ligne])
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe(() => {
        this.formPonctuelle.reset({ clientId });
        this.ponctuelle.set(false);
        this.recharger();
      });
  }

  protected libelleAction(facture: Facture, action: ActionFacture): string {
    if (action === 'valider') return 'Valider';
    if (action === 'payee') return 'Payée';
    return facture.statut === 'DEPOT_EN_COURS' ? 'Réessayer' : 'Déposer';
  }

  protected agir(facture: Facture, action: ActionFacture): void {
    const question = CONFIRMATIONS[action];
    if ((question && !confirm(question)) || !this.commencer()) return;
    this.api
      .actionFacture(facture.id, action)
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe((resultat) => {
        if (resultat.statut === 'DEPOT_EN_COURS') {
          this.message.set(
            "La plateforme n'a pas répondu : le dépôt est conservé et sera retenté automatiquement.",
          );
        }
        this.recharger();
      });
  }

  protected supprimer(facture: Facture): void {
    if (!confirm('Supprimer ce brouillon ?') || !this.commencer()) return;
    this.api
      .supprimerFacture(facture.id)
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe(() => this.recharger());
  }

  protected telecharger(facture: Facture): void {
    if (!this.commencer()) return;
    this.api
      .facturX(facture.id)
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe((pdf) => {
        const url = URL.createObjectURL(pdf);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `${facture.numero}.pdf`;
        lien.click();
        URL.revokeObjectURL(url);
      });
  }

  /** Refuse une action tant que la précédente n'a pas répondu (double clic) ; efface le message précédent. */
  private commencer(): boolean {
    if (this.enCours()) return false;
    this.enCours.set(true);
    this.message.set('');
    return true;
  }

  private recharger(): void {
    this.api.factures().subscribe((factures) => this.factures.set(factures));
  }
}
