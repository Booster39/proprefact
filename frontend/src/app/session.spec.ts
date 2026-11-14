import { TestBed } from '@angular/core/testing';
import { Session } from './session';

describe('Session', () => {
  beforeEach(() => localStorage.clear());

  it('ouvre puis ferme la session', () => {
    const session = TestBed.inject(Session);
    expect(session.connecte()).toBe(false);
    session.ouvrir({ jeton: 'abc', raisonSociale: 'Net Plus' });
    expect(session.connecte()).toBe(true);
    expect(session.jeton()).toBe('abc');
    session.fermer();
    expect(session.connecte()).toBe(false);
  });
});
