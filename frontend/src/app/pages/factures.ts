import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Api } from '../api';
import { Client, Facture, LIBELLES_STATUT, Ligne, dateFr, messageErreur, montant } from '../modeles';

@Component({
  imports: [FormsModule],
  templateUrl: './factures.html',
})
export class Factures {
  private readonly api = inject(Api);
  protected readonly montant = montant;
  protected readonly dateFr = dateFr;
  protected readonly libelles = LIBELLES_STATUT;

  protected readonly factures = signal<Facture[]>([]);
  protected readonly clients = signal<Client[]>([]);
  protected readonly erreur = signal('');
  protected readonly message = signal('');
  protected readonly ponctuelle = signal(false);
  protected mois = new Date().toISOString().slice(0, 7);
  protected clientId: number | null = null;
  protected ligne: Ligne = this.ligneVide();

  constructor() {
    this.recharger();
    this.api.clients().subscribe({ next: (c) => this.clients.set(c), error: (e) => this.echec(e) });
  }

  protected generer(): void {
    this.debut();
    this.api.genererMensuelles(this.mois).subscribe({
      next: (creees) => {
        this.message.set(creees.length === 0
          ? 'Rien à facturer : tous les contrats actifs ce mois-là sont déjà facturés.'
          : `${creees.length} brouillon(s) créé(s). Vérifiez-les puis validez-les.`);
        this.recharger();
      },
      error: (e) => this.echec(e),
    });
  }

  protected creerPonctuelle(form: NgForm): void {
    if (this.clientId === null) return;
    this.debut();
    this.api.creerFacture(this.clientId, [this.ligne]).subscribe({
      next: () => {
        this.ligne = this.ligneVide();
        form.resetForm({ ...this.ligne, clientId: this.clientId });
        this.ponctuelle.set(false);
        this.recharger();
      },
      error: (e) => this.echec(e),
    });
  }

  protected action(f: Facture, action: 'valider' | 'deposer' | 'payee'): void {
    this.debut();
    this.api.actionFacture(f.id, action).subscribe({
      next: (maj) => {
        if (maj.statut === 'DEPOT_EN_COURS') {
          this.message.set('La plateforme n\'a pas répondu : le dépôt est conservé et sera retenté automatiquement.');
        }
        this.recharger();
      },
      error: (e) => this.echec(e),
    });
  }

  protected supprimer(f: Facture): void {
    this.debut();
    this.api.supprimerFacture(f.id).subscribe({ next: () => this.recharger(), error: (e) => this.echec(e) });
  }

  protected telecharger(f: Facture): void {
    this.debut();
    this.api.facturX(f.id).subscribe({
      next: (pdf) => {
        const url = URL.createObjectURL(pdf);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `${f.numero}.pdf`;
        lien.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => this.echec(e),
    });
  }

  private recharger(): void {
    this.api.factures().subscribe({ next: (f) => this.factures.set(f), error: (e) => this.echec(e) });
  }

  private debut(): void {
    this.erreur.set('');
    this.message.set('');
  }

  private echec(e: unknown): void {
    this.erreur.set(messageErreur(e));
  }

  private ligneVide(): Ligne {
    return { designation: '', quantite: 1, prixUnitaireHt: 0, tauxTva: 20 };
  }
}
