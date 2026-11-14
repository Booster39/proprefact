import { TestBed } from '@angular/core/testing';
import { Subject, of } from 'rxjs';
import { Api } from '../api/api';
import { Client, Site } from '../api/modeles';
import { Clients } from './clients';

const client = (id: number, raisonSociale: string): Client => ({
  id,
  raisonSociale,
  siren: '123456789',
  tvaIntra: null,
  adresse: '1 rue A',
  codePostal: '75001',
  ville: 'Paris',
  email: null,
});

const site = (id: number, clientId: number, nom: string): Site => ({
  id,
  clientId,
  nom,
  adresse: '1 rue A',
  codePostal: '75001',
  ville: 'Paris',
});

describe('Clients', () => {
  it("n'affiche pas les sites d'un client précédent quand sa réponse arrive en retard", async () => {
    const reponses = new Map<number, Subject<Site[]>>([
      [1, new Subject<Site[]>()],
      [2, new Subject<Site[]>()],
    ]);
    const api = {
      clients: () => of([client(1, 'Alpha'), client(2, 'Bravo')]),
      sites: (id: number) => reponses.get(id)!.asObservable(),
    };
    TestBed.configureTestingModule({ providers: [{ provide: Api, useValue: api }] });
    const fixture = TestBed.createComponent(Clients);
    await fixture.whenStable();
    const page: HTMLElement = fixture.nativeElement;
    const boutons = Array.from(page.querySelectorAll<HTMLButtonElement>('tbody button'));

    boutons[0].click(); // Alpha
    boutons[1].click(); // Bravo, avant que la réponse d'Alpha n'arrive
    reponses.get(2)!.next([site(20, 2, 'Site de Bravo')]);
    reponses.get(1)!.next([site(10, 1, "Site d'Alpha")]);
    await fixture.whenStable();

    expect(page.textContent).toContain('Sites de Bravo');
    expect(page.textContent).toContain('Site de Bravo');
    expect(page.textContent).not.toContain("Site d'Alpha");
  });
});
