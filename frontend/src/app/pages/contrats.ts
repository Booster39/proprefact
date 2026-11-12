import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Api } from '../api';
import { Client, Contrat, Site, dateFr, messageErreur, montant } from '../modeles';

@Component({
  imports: [FormsModule],
  templateUrl: './contrats.html',
})
export class Contrats {
  private readonly api = inject(Api);
  protected readonly montant = montant;
  protected readonly dateFr = dateFr;

  protected readonly contrats = signal<Contrat[]>([]);
  protected readonly clients = signal<Client[]>([]);
  protected readonly sites = signal<Site[]>([]);
  protected readonly erreur = signal('');
  protected clientId: number | null = null;
  protected nouveau = this.vide();

  constructor() {
    const echec = (e: unknown) => this.erreur.set(messageErreur(e));
    this.api.contrats().subscribe({ next: (c) => this.contrats.set(c), error: echec });
    this.api.clients().subscribe({ next: (c) => this.clients.set(c), error: echec });
  }

  protected chargerSites(): void {
    this.sites.set([]);
    this.nouveau.siteId = null;
    if (this.clientId !== null) {
      this.api.sites(this.clientId).subscribe({ next: (s) => this.sites.set(s), error: (e) => this.erreur.set(messageErreur(e)) });
    }
  }

  protected creer(form: NgForm): void {
    this.erreur.set('');
    this.api.creerContrat({ ...this.nouveau, dateFin: this.nouveau.dateFin || null }).subscribe({
      next: (c) => {
        this.contrats.update((liste) => [...liste, c]);
        const siteId = this.nouveau.siteId;
        this.nouveau = { ...this.vide(), siteId };
        form.resetForm({ ...this.nouveau, clientId: this.clientId });
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  protected resilier(c: Contrat, dateFin: string): void {
    if (!dateFin) return;
    this.erreur.set('');
    this.api.resilier(c.id, dateFin).subscribe({
      next: (maj) => this.contrats.update((liste) => liste.map((x) => (x.id === maj.id ? maj : x))),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  private vide() {
    return {
      siteId: null as number | null, libelle: '', montantMensuelHt: null as number | null, tauxTva: 20,
      dateDebut: new Date().toISOString().slice(0, 10), dateFin: '',
    };
  }
}
