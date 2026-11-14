import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, finalize, switchMap } from 'rxjs';
import { Api } from '../api/api';
import { Client, Site } from '../api/modeles';

@Component({
  imports: [ReactiveFormsModule],
  templateUrl: './clients.html',
})
export class Clients {
  private readonly api = inject(Api);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly clientChoisi = new Subject<number>();

  protected readonly clients = signal<Client[]>([]);
  protected readonly selection = signal<Client | null>(null);
  protected readonly sites = signal<Site[]>([]);
  protected readonly enCours = signal(false);

  // Les tailles maximales sont celles que le back-end accepte (service/commande).
  protected readonly formClient = this.fb.group({
    raisonSociale: ['', [Validators.required, Validators.maxLength(200)]],
    siren: ['', [Validators.required, Validators.pattern(/^[0-9]{9}$/)]],
    tvaIntra: ['', Validators.maxLength(20)],
    adresse: ['', [Validators.required, Validators.maxLength(200)]],
    codePostal: ['', [Validators.required, Validators.maxLength(10)]],
    ville: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(200)]],
  });

  protected readonly formSite = this.fb.group({
    nom: ['', [Validators.required, Validators.maxLength(200)]],
    adresse: ['', [Validators.required, Validators.maxLength(200)]],
    codePostal: ['', [Validators.required, Validators.maxLength(10)]],
    ville: ['', [Validators.required, Validators.maxLength(100)]],
  });

  constructor() {
    this.api.clients().subscribe((clients) => this.clients.set(clients));

    // switchMap abandonne la réponse d'un client précédent si l'utilisateur en a choisi un autre.
    this.clientChoisi
      .pipe(
        switchMap((id) => this.api.sites(id)),
        takeUntilDestroyed(),
      )
      .subscribe((sites) => this.sites.set(sites));
  }

  protected creer(): void {
    if (this.formClient.invalid || this.enCours()) return;
    this.enCours.set(true);
    this.api
      .creerClient(this.formClient.getRawValue())
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe((client) => {
        this.clients.update((liste) =>
          [...liste, client].sort((a, b) => a.raisonSociale.localeCompare(b.raisonSociale)),
        );
        this.formClient.reset();
        this.choisir(client);
      });
  }

  protected choisir(client: Client): void {
    this.selection.set(client);
    this.sites.set([]);
    this.clientChoisi.next(client.id);
  }

  protected ajouterSite(): void {
    const client = this.selection();
    if (!client || this.formSite.invalid || this.enCours()) return;
    this.enCours.set(true);
    this.api
      .creerSite(client.id, this.formSite.getRawValue())
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe((site) => {
        this.sites.update((liste) => [...liste, site]);
        this.formSite.reset();
      });
  }
}
