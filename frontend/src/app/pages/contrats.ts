import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { filter, finalize, switchMap, tap } from 'rxjs';
import { Api } from '../api/api';
import { Client, Contrat, Site } from '../api/modeles';
import { DateFrPipe } from '../commun/date-fr.pipe';
import { aujourdhui } from '../commun/formats';
import { MontantPipe } from '../commun/montant.pipe';

@Component({
  imports: [ReactiveFormsModule, MontantPipe, DateFrPipe],
  templateUrl: './contrats.html',
})
export class Contrats {
  private readonly api = inject(Api);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly contrats = signal<Contrat[]>([]);
  protected readonly clients = signal<Client[]>([]);
  protected readonly sites = signal<Site[]>([]);
  protected readonly enCours = signal(false);

  // clientId ne sert qu'à choisir le site : il n'est pas envoyé au back-end.
  protected readonly form = this.fb.group({
    clientId: this.fb.control<number | null>(null, Validators.required),
    siteId: this.fb.control<number | null>(null, Validators.required),
    libelle: ['', [Validators.required, Validators.maxLength(300)]],
    montantMensuelHt: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
    tauxTva: [20, [Validators.required, Validators.min(0), Validators.max(100)]],
    dateDebut: [aujourdhui(), Validators.required],
    dateFin: [''],
  });

  constructor() {
    this.api.contrats().subscribe((contrats) => this.contrats.set(contrats));
    this.api.clients().subscribe((clients) => this.clients.set(clients));

    this.form.controls.clientId.valueChanges
      .pipe(
        tap(() => {
          this.sites.set([]);
          this.form.controls.siteId.reset();
        }),
        filter((id) => id !== null),
        // switchMap abandonne la réponse d'un client précédent si l'utilisateur en a choisi un autre.
        switchMap((id) => this.api.sites(id)),
        takeUntilDestroyed(),
      )
      .subscribe((sites) => this.sites.set(sites));
  }

  protected creer(): void {
    const { clientId, siteId, montantMensuelHt, dateFin, ...reste } = this.form.getRawValue();
    if (this.form.invalid || siteId === null || montantMensuelHt === null || this.enCours()) return;
    this.enCours.set(true);
    this.api
      .creerContrat({ ...reste, siteId, montantMensuelHt, dateFin: dateFin || null })
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe((contrat) => {
        this.contrats.update((liste) => [...liste, contrat]);
        // Le client et le site restent choisis pour enchaîner les contrats d'un même site.
        // emitEvent: false, sinon le changement de client viderait la liste des sites.
        this.form.reset({ clientId, siteId }, { emitEvent: false });
      });
  }

  protected resilier(contrat: Contrat, dateFin: string): void {
    if (!dateFin || this.enCours()) return;
    this.enCours.set(true);
    this.api
      .resilier(contrat.id, dateFin)
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe((resilie) =>
        this.contrats.update((liste) => liste.map((c) => (c.id === resilie.id ? resilie : c))),
      );
  }
}
