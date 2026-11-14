import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ActionFacture,
  Client,
  Contrat,
  Facture,
  Inscription,
  Jeton,
  NouveauClient,
  NouveauContrat,
  NouveauSite,
  NouvelleLigne,
  Site,
} from './modeles';
import { ERREUR_TRANSMISE } from './erreurs';

/** Pour les deux appels dont l'écran réagit lui-même à l'échec (compte non confirmé, lien expiré). */
const AVEC_ERREUR = { context: new HttpContext().set(ERREUR_TRANSMISE, true) };

/** Appels HTTP vers le back-end, un par opération de l'API. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  inscription(donnees: Inscription): Observable<void> {
    return this.http.post<void>('/api/auth/inscription', donnees);
  }
  confirmer(jeton: string): Observable<Jeton> {
    return this.http.post<Jeton>('/api/auth/confirmation', { jeton }, AVEC_ERREUR);
  }
  renvoyerConfirmation(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/renvoi-confirmation', { email });
  }
  connexion(email: string, motDePasse: string): Observable<Jeton> {
    return this.http.post<Jeton>('/api/auth/connexion', { email, motDePasse }, AVEC_ERREUR);
  }

  clients(): Observable<Client[]> {
    return this.http.get<Client[]>('/api/clients');
  }
  creerClient(c: NouveauClient): Observable<Client> {
    return this.http.post<Client>('/api/clients', c);
  }
  sites(clientId: number): Observable<Site[]> {
    return this.http.get<Site[]>(`/api/clients/${clientId}/sites`);
  }
  creerSite(clientId: number, s: NouveauSite): Observable<Site> {
    return this.http.post<Site>(`/api/clients/${clientId}/sites`, s);
  }

  contrats(): Observable<Contrat[]> {
    return this.http.get<Contrat[]>('/api/contrats');
  }
  creerContrat(c: NouveauContrat): Observable<Contrat> {
    return this.http.post<Contrat>('/api/contrats', c);
  }
  resilier(id: number, dateFin: string): Observable<Contrat> {
    return this.http.post<Contrat>(`/api/contrats/${id}/resilier`, null, { params: { dateFin } });
  }

  factures(): Observable<Facture[]> {
    return this.http.get<Facture[]>('/api/factures');
  }
  creerFacture(clientId: number, lignes: NouvelleLigne[]): Observable<Facture> {
    return this.http.post<Facture>('/api/factures', { clientId, lignes });
  }
  genererMensuelles(mois: string): Observable<Facture[]> {
    return this.http.post<Facture[]>('/api/factures/generer-mensuelles', null, {
      params: { mois },
    });
  }
  actionFacture(id: number, action: ActionFacture): Observable<Facture> {
    return this.http.post<Facture>(`/api/factures/${id}/${action}`, null);
  }
  supprimerFacture(id: number): Observable<void> {
    return this.http.delete<void>(`/api/factures/${id}`);
  }
  facturX(id: number): Observable<Blob> {
    return this.http.get(`/api/factures/${id}/facturx`, { responseType: 'blob' });
  }
}
