import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Api } from '../api';
import { Client, Site, messageErreur } from '../modeles';

@Component({
  imports: [FormsModule],
  templateUrl: './clients.html',
})
export class Clients {
  private readonly api = inject(Api);

  protected readonly clients = signal<Client[]>([]);
  protected readonly selection = signal<Client | null>(null);
  protected readonly sites = signal<Site[]>([]);
  protected readonly erreur = signal('');
  protected nouveau = this.clientVide();
  protected nouveauSite = this.siteVide();

  constructor() {
    this.api.clients().subscribe({ next: (c) => this.clients.set(c), error: (e) => this.erreur.set(messageErreur(e)) });
  }

  protected creer(form: NgForm): void {
    this.erreur.set('');
    this.api.creerClient(this.nouveau).subscribe({
      next: (c) => {
        this.clients.update((liste) => [...liste, c].sort((a, b) => a.raisonSociale.localeCompare(b.raisonSociale)));
        this.nouveau = this.clientVide();
        form.resetForm(this.nouveau);
        this.choisir(c);
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  protected choisir(c: Client): void {
    this.selection.set(c);
    this.sites.set([]);
    this.api.sites(c.id).subscribe({ next: (s) => this.sites.set(s), error: (e) => this.erreur.set(messageErreur(e)) });
  }

  protected ajouterSite(form: NgForm): void {
    const client = this.selection();
    if (!client) return;
    this.erreur.set('');
    this.api.creerSite(client.id, this.nouveauSite).subscribe({
      next: (s) => {
        this.sites.update((liste) => [...liste, s]);
        this.nouveauSite = this.siteVide();
        form.resetForm(this.nouveauSite);
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  private clientVide() {
    return { raisonSociale: '', siren: '', tvaIntra: '', adresse: '', codePostal: '', ville: '', email: '' };
  }

  private siteVide() {
    return { nom: '', adresse: '', codePostal: '', ville: '' };
  }
}
