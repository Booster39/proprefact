export interface Jeton {
  jeton: string;
  raisonSociale: string;
}

export interface Client {
  id: number;
  raisonSociale: string;
  siren: string;
  tvaIntra: string | null;
  adresse: string;
  codePostal: string;
  ville: string;
  email: string | null;
}

export interface Site {
  id: number;
  clientId: number;
  nom: string;
  adresse: string;
  codePostal: string;
  ville: string;
}

export interface Contrat {
  id: number;
  siteId: number;
  site: string;
  client: string;
  libelle: string;
  montantMensuelHt: number;
  tauxTva: number;
  dateDebut: string;
  dateFin: string | null;
}

export type StatutFacture = 'BROUILLON' | 'VALIDEE' | 'DEPOT_EN_COURS' | 'DEPOSEE' | 'PAYEE';

export const LIBELLES_STATUT: Record<StatutFacture, string> = {
  BROUILLON: 'Brouillon',
  VALIDEE: 'Validée',
  DEPOT_EN_COURS: 'Dépôt en cours',
  DEPOSEE: 'Déposée',
  PAYEE: 'Payée',
};

export interface Ligne {
  designation: string;
  quantite: number;
  prixUnitaireHt: number;
  tauxTva: number;
  montantHt?: number;
}

export interface Facture {
  id: number;
  numero: string | null;
  statut: StatutFacture;
  clientId: number;
  client: string;
  dateEmission: string | null;
  dateEcheance: string | null;
  periode: string | null;
  totalHt: number;
  totalTva: number;
  totalTtc: number;
  idDepot: string | null;
  depotErreur: string | null;
  lignes: Ligne[] | null;
}

const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });
export const montant = (m: number) => euros.format(m);
export const dateFr = (d: string | null) => (d ? d.split('-').reverse().join('/') : '—');

/** Date du jour dans le fuseau du navigateur, au format AAAA-MM-JJ (toISOString donnerait la date UTC). */
export function aujourdhui(): string {
  const d = new Date();
  const deuxChiffres = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${deuxChiffres(d.getMonth() + 1)}-${deuxChiffres(d.getDate())}`;
}

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
