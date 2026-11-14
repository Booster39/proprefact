import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Api } from '../api/api';
import { Facture, StatutFacture } from '../api/modeles';
import { Factures } from './factures';

const facture = (id: number, statut: StatutFacture, numero: string | null): Facture => ({
  id,
  numero,
  statut,
  clientId: 1,
  client: 'Cabinet Durand',
  dateEmission: numero ? '2026-11-14' : null,
  dateEcheance: numero ? '2026-12-14' : null,
  periode: '2026-11',
  totalHt: 1480,
  totalTva: 296,
  totalTtc: 1776,
  idDepot: null,
  depotErreur: statut === 'DEPOT_EN_COURS' ? 'Plateforme injoignable' : null,
  lignes: null,
});

describe('Factures', () => {
  const api = {
    clients: vi.fn(() => of([])),
    factures: vi.fn(),
    actionFacture: vi.fn(),
    supprimerFacture: vi.fn(() => of(undefined)),
  };

  async function afficher(factures: Facture[]): Promise<HTMLElement> {
    api.factures.mockReturnValue(of(factures));
    TestBed.configureTestingModule({ providers: [{ provide: Api, useValue: api }] });
    const fixture = TestBed.createComponent(Factures);
    await fixture.whenStable();
    return fixture.nativeElement;
  }

  const boutons = (ligne: Element) =>
    Array.from(ligne.querySelectorAll('button')).map((b) => b.textContent?.trim());

  beforeEach(() => {
    vi.clearAllMocks();
    vi.restoreAllMocks();
  });

  it('propose pour chaque statut les seules actions permises', async () => {
    const page = await afficher([
      facture(1, 'BROUILLON', null),
      facture(2, 'VALIDEE', 'F-2026-00001'),
      facture(3, 'DEPOT_EN_COURS', 'F-2026-00002'),
      facture(4, 'DEPOSEE', 'F-2026-00003'),
      facture(5, 'PAYEE', 'F-2026-00004'),
    ]);
    const lignes = Array.from(page.querySelectorAll('tbody tr'));

    expect(boutons(lignes[0])).toEqual(['Valider', 'Supprimer']);
    expect(boutons(lignes[1])).toEqual(['Factur-X', 'Déposer', 'Payée']);
    expect(boutons(lignes[2])).toEqual(['Factur-X', 'Réessayer']);
    expect(boutons(lignes[3])).toEqual(['Factur-X', 'Payée']);
    expect(boutons(lignes[4])).toEqual(['Factur-X']);
  });

  it('affiche les montants et les dates au format français', async () => {
    const page = await afficher([facture(2, 'VALIDEE', 'F-2026-00001')]);
    const texte = page.querySelector('tbody tr')?.textContent?.replace(/\s+/g, ' ');

    expect(texte).toContain('F-2026-00001');
    expect(texte).toContain('14/11/2026');
    expect(texte).toContain('1 480,00 €');
    expect(texte).toContain('1 776,00 €');
    expect(texte).toContain('Validée');
  });

  it("montre la dernière erreur d'un dépôt en cours", async () => {
    const page = await afficher([facture(3, 'DEPOT_EN_COURS', 'F-2026-00002')]);
    expect(page.textContent).toContain('Dernier essai : Plateforme injoignable');
  });

  it("ne valide qu'après confirmation", async () => {
    const page = await afficher([facture(1, 'BROUILLON', null)]);
    api.actionFacture.mockReturnValue(of(facture(1, 'VALIDEE', 'F-2026-00001')));
    const valider = page.querySelector<HTMLButtonElement>('tbody button')!;

    vi.spyOn(window, 'confirm').mockReturnValue(false);
    valider.click();
    expect(api.actionFacture).not.toHaveBeenCalled();

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    valider.click();
    expect(api.actionFacture).toHaveBeenCalledWith(1, 'valider');
  });
});
