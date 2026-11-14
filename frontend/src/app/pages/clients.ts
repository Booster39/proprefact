import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NgForm } from '@angular/forms';
import { Subject, catchError, of, switchMap } from 'rxjs';
import { Api } from '../api';
import { Client, Site, messageErreur } from '../modeles';

@Component({
  imports: [FormsModule],
  templateUrl: './clients.html',
})
export class Clients {
  private readonly api = inject(Api);
  private readonly clientChoisi = new Subject<number>();

  protected readonly clients = signal<Client[]>([]);
  protected readonly selection = signal<Client | null>(null);
  protected readonly sites = signal<Site[]>([]);
  protected readonly erreur = signal('');
  protected nouveau = this.clientVide();
  protected nouveauSite = this.siteVide();

  constructor() {
    this.api.clients().subscribe({
      next: (c) => this.clients.set(c),
      error: (e) => this.erreur.set(messageErreur(e)),
    });

    // switchMap abandonne la réponse d'un client précédent si l'utilisateur en a choisi un autre.
    this.clientChoisi
      .pipe(
        switchMap((id) =>
          this.api.sites(id).pipe(
            // Dans le switchMap : une erreur ne doit pas arrêter les chargements suivants.
            catchError((e) => {
              this.erreur.set(messageErreur(e));
              return of([] as Site[]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((sites) => this.sites.set(sites));
  }

  protected creer(form: NgForm): void {
    this.erreur.set('');
    this.api.creerClient(this.nouveau).subscribe({
      next: (c) => {
        this.clients.update((liste) =>
          [...liste, c].sort((a, b) => a.raisonSociale.localeCompare(b.raisonSociale)),
        );
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
    this.clientChoisi.next(c.id);
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
    return {
      raisonSociale: '',
      siren: '',
      tvaIntra: '',
      adresse: '',
      codePostal: '',
      ville: '',
      email: '',
    };
  }

  private siteVide() {
    return { nom: '', adresse: '', codePostal: '', ville: '' };
  }
}
