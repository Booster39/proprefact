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

  it('détaille les erreurs par champ', () => {
    const erreur = {
      status: 400,
      error: {
        detail: 'Données invalides',
        erreurs: { siren: '9 chiffres attendus', 'lignes[0].quantite': 'obligatoire' },
      },
    };
    expect(messageErreur(erreur)).toBe('SIREN : 9 chiffres attendus — Quantité : obligatoire');
  });
});
