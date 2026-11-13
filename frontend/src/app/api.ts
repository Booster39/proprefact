import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, catchError, throwError } from 'rxjs';
import { Client, Contrat, Facture, Jeton, Ligne, Site } from './modeles';
import { Session } from './session';

/** Ajoute le jeton aux appels et renvoie à la connexion s'il a expiré. */
export const intercepteurJeton: HttpInterceptorFn = (req, next) => {
  const session = inject(Session);
  const router = inject(Router);
  const jeton = session.jeton();
  const requete = jeton ? req.clone({ setHeaders: { Authorization: `Bearer ${jeton}` } }) : req;
  return next(requete).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && !req.url.includes('/api/auth/')) {
        session.fermer();
        router.navigateByUrl('/connexion');
      }
      return throwError(() => e);
    }),
  );
};

export const connexionRequise: CanActivateFn = () =>
  inject(Session).connecte() || inject(Router).parseUrl('/connexion');

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  inscription(donnees: object): Observable<void> { return this.http.post<void>('/api/auth/inscription', donnees); }
  confirmer(jeton: string): Observable<Jeton> { return this.http.post<Jeton>('/api/auth/confirmation', { jeton }); }
  renvoyerConfirmation(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/renvoi-confirmation', { email });
  }
  connexion(email: string, motDePasse: string): Observable<Jeton> {
    return this.http.post<Jeton>('/api/auth/connexion', { email, motDePasse });
  }

  clients(): Observable<Client[]> { return this.http.get<Client[]>('/api/clients'); }
  creerClient(c: object): Observable<Client> { return this.http.post<Client>('/api/clients', c); }
  sites(clientId: number): Observable<Site[]> { return this.http.get<Site[]>(`/api/clients/${clientId}/sites`); }
  creerSite(clientId: number, s: object): Observable<Site> {
    return this.http.post<Site>(`/api/clients/${clientId}/sites`, s);
  }

  contrats(): Observable<Contrat[]> { return this.http.get<Contrat[]>('/api/contrats'); }
  creerContrat(c: object): Observable<Contrat> { return this.http.post<Contrat>('/api/contrats', c); }
  resilier(id: number, dateFin: string): Observable<Contrat> {
    return this.http.post<Contrat>(`/api/contrats/${id}/resilier`, null, { params: { dateFin } });
  }

  factures(): Observable<Facture[]> { return this.http.get<Facture[]>('/api/factures'); }
  creerFacture(clientId: number, lignes: Ligne[]): Observable<Facture> {
    return this.http.post<Facture>('/api/factures', { clientId, lignes });
  }
  genererMensuelles(mois: string): Observable<Facture[]> {
    return this.http.post<Facture[]>('/api/factures/generer-mensuelles', null, { params: { mois } });
  }
  actionFacture(id: number, action: 'valider' | 'deposer' | 'payee'): Observable<Facture> {
    return this.http.post<Facture>(`/api/factures/${id}/${action}`, null);
  }
  supprimerFacture(id: number): Observable<void> { return this.http.delete<void>(`/api/factures/${id}`); }
  facturX(id: number): Observable<Blob> { return this.http.get(`/api/factures/${id}/facturx`, { responseType: 'blob' }); }
}
