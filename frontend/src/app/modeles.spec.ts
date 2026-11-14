import { messageErreur } from './modeles';

describe('messageErreur', () => {
  it('reprend le détail renvoyé par le serveur', () => {
    expect(messageErreur({ status: 409, error: { detail: 'Facture introuvable' } })).toBe(
      'Facture introuvable',
    );
  });

  it('signale un serveur injoignable', () => {
    expect(messageErreur({ status: 0 })).toBe('Serveur injoignable.');
  });
});
