import { HttpContextToken } from '@angular/common/http';

/**
 * À poser sur une requête dont l'écran veut recevoir l'erreur lui-même (en plus de son affichage).
 * Par défaut, l'intercepteur affiche l'erreur et termine la requête sans rien transmettre.
 */
export const ERREUR_TRANSMISE = new HttpContextToken<boolean>(() => false);

/** Nom affiché de chaque champ que le back-end peut signaler comme invalide. */
const LIBELLES_CHAMP: Record<string, string> = {
  raisonSociale: 'Raison sociale',
  siren: 'SIREN',
  tvaIntra: 'N° de TVA',
  adresse: 'Adresse',
  codePostal: 'Code postal',
  ville: 'Ville',
  email: 'E-mail',
  motDePasse: 'Mot de passe',
  iban: 'IBAN',
  bic: 'BIC',
  nom: 'Nom du site',
  libelle: 'Prestation',
  montantMensuelHt: 'Montant mensuel HT',
  tauxTva: 'TVA',
  dateDebut: 'Début',
  designation: 'Désignation',
  quantite: 'Quantité',
  prixUnitaireHt: 'Prix unitaire HT',
};

interface ErreurHttp {
  status?: number;
  error?: { detail?: string; erreurs?: Record<string, string> };
}

/** Message lisible à partir d'une erreur HTTP du back-end (format « problem detail »). */
export function messageErreur(e: unknown): string {
  const err = e as ErreurHttp;
  const parChamp = Object.entries(err?.error?.erreurs ?? {}).map(([champ, message]) => {
    // « lignes[0].prixUnitaireHt » → « prixUnitaireHt »
    const nom = champ.split('.').pop() ?? champ;
    return `${LIBELLES_CHAMP[nom] ?? nom} : ${message}`;
  });
  if (parChamp.length > 0) return parChamp.join(' — ');
  if (err?.error?.detail) return err.error.detail;
  if (err?.status === 0) return 'Serveur injoignable.';
  if (err?.status === 429) return 'Trop de tentatives : réessayez dans quelques minutes.';
  return 'Une erreur est survenue.';
}
