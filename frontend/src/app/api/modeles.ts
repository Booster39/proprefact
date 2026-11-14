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
  montantHt: number;
}

export type ActionFacture = 'valider' | 'deposer' | 'payee';

/** Actions proposées pour chaque statut ; doit rester aligné sur les règles du back-end (Facture.java). */
export const ACTIONS_PAR_STATUT: Record<StatutFacture, ActionFacture[]> = {
  BROUILLON: ['valider'],
  VALIDEE: ['deposer', 'payee'],
  DEPOT_EN_COURS: ['deposer'],
  DEPOSEE: ['payee'],
  PAYEE: [],
};

/** Classe CSS de l'étiquette de chaque statut (styles.css). */
export const CLASSE_STATUT: Record<StatutFacture, string> = {
  BROUILLON: 'statut-brouillon',
  VALIDEE: 'statut-validee',
  DEPOT_EN_COURS: 'statut-depot-en-cours',
  DEPOSEE: 'statut-deposee',
  PAYEE: 'statut-payee',
};

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

// Objets envoyés au back-end. Leurs champs sont ceux des « commandes » Java (service/commande).

export interface NouveauClient {
  raisonSociale: string;
  siren: string;
  tvaIntra: string;
  adresse: string;
  codePostal: string;
  ville: string;
  email: string;
}

export interface NouveauSite {
  nom: string;
  adresse: string;
  codePostal: string;
  ville: string;
}

export interface NouveauContrat {
  siteId: number;
  libelle: string;
  montantMensuelHt: number;
  tauxTva: number;
  dateDebut: string;
  dateFin: string | null;
}

export interface NouvelleLigne {
  designation: string;
  quantite: number;
  prixUnitaireHt: number;
  tauxTva: number;
}

export interface Inscription {
  email: string;
  motDePasse: string;
  raisonSociale: string;
  siren: string;
  tvaIntra: string;
  adresse: string;
  codePostal: string;
  ville: string;
  iban: string;
  bic: string;
  optionTvaDebits: boolean;
}
