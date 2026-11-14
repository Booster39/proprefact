import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { connexionRequise } from './connexion-requise';
import { Session } from './session';

describe('connexionRequise', () => {
  const executer = () =>
    TestBed.runInInjectionContext(() =>
      connexionRequise({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => localStorage.clear());

  it('laisse passer un utilisateur connecté', () => {
    TestBed.inject(Session).ouvrir({ jeton: 'abc', raisonSociale: 'Net Plus' });
    expect(executer()).toBe(true);
  });

  it('renvoie les autres à la connexion', () => {
    const resultat = executer();
    expect(resultat).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(resultat as UrlTree)).toBe('/connexion');
  });
});
