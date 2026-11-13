export interface Jeton { jeton: string; raisonSociale: string; }

export interface Client {
  id: number; raisonSociale: string; siren: string; tvaIntra: string | null;
  adresse: string; codePostal: string; ville: string; email: string | null;
}

export interface Site { id: number; clientId: number; nom: string; adresse: string; codePostal: string; ville: string; }

export interface Contrat {
  id: number; siteId: number; site: string; client: string; libelle: string;
  montantMensuelHt: number; tauxTva: number; dateDebut: string; dateFin: string | null;
}

export type StatutFacture = 'BROUILLON' | 'VALIDEE' | 'DEPOSEE' | 'PAYEE';

export interface Ligne { designation: string; quantite: number; prixUnitaireHt: number; tauxTva: number; montantHt?: number; }

export interface Facture {
  id: number; numero: string | null; statut: StatutFacture; clientId: number; client: string;
  dateEmission: string | null; dateEcheance: string | null; periode: string | null;
  totalHt: number; totalTva: number; totalTtc: number; idDepot: string | null; lignes: Ligne[] | null;
}

const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });
export const montant = (m: number) => euros.format(m);
export const dateFr = (d: string | null) => (d ? d.split('-').reverse().join('/') : '—');

/** Message lisible à partir d'une erreur HTTP du back-end (format « problem detail »). */
export function messageErreur(e: unknown): string {
  const err = e as { status?: number; error?: { detail?: string } };
  if (err?.error?.detail) return err.error.detail;
  if (err?.status === 0) return 'Serveur injoignable.';
  if (err?.status === 429) return 'Trop de tentatives : réessayez dans quelques minutes.';
  if (err?.status === 400) return 'Données invalides : vérifiez les champs.';
  return 'Une erreur est survenue.';
}
