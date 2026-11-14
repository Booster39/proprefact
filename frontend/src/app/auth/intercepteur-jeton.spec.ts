import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ERREUR_TRANSMISE } from '../api/erreurs';
import { Notifications } from '../commun/notifications';
import { intercepteurJeton } from './intercepteur-jeton';
import { Session } from './session';

describe('intercepteurJeton', () => {
  let http: HttpClient;
  let serveur: HttpTestingController;
  let session: Session;
  let notifications: Notifications;
  const router = { navigateByUrl: vi.fn() };

  beforeEach(() => {
    localStorage.clear();
    router.navigateByUrl.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([intercepteurJeton])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    serveur = TestBed.inject(HttpTestingController);
    session = TestBed.inject(Session);
    notifications = TestBed.inject(Notifications);
  });

  afterEach(() => serveur.verify());

  it('ajoute le jeton quand une session est ouverte, et rien sinon', () => {
    http.get('/api/clients').subscribe();
    expect(serveur.expectOne('/api/clients').request.headers.has('Authorization')).toBe(false);

    session.ouvrir({ jeton: 'abc', raisonSociale: 'Net Plus' });
    http.get('/api/factures').subscribe();
    expect(serveur.expectOne('/api/factures').request.headers.get('Authorization')).toBe(
      'Bearer abc',
    );
  });

  it('ferme la session et renvoie à la connexion quand le jeton a expiré', () => {
    session.ouvrir({ jeton: 'abc', raisonSociale: 'Net Plus' });
    http.get('/api/factures').subscribe();
    serveur.expectOne('/api/factures').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(session.connecte()).toBe(false);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/connexion');
    expect(notifications.erreur()).toBe('');
  });

  it("affiche l'erreur et termine la requête sans la transmettre à l'écran", () => {
    const erreurRecue = vi.fn();
    const terminee = vi.fn();
    http.post('/api/clients', {}).subscribe({ error: erreurRecue, complete: terminee });
    serveur
      .expectOne('/api/clients')
      .flush({ detail: 'Client introuvable' }, { status: 404, statusText: 'Not Found' });

    expect(notifications.erreur()).toBe('Client introuvable');
    expect(erreurRecue).not.toHaveBeenCalled();
    expect(terminee).toHaveBeenCalled();
  });

  it("transmet l'erreur à l'écran qui la demande, sans fermer la session sur un 401 de connexion", () => {
    const erreurRecue = vi.fn();
    const contexte = new HttpContext().set(ERREUR_TRANSMISE, true);
    http.post('/api/auth/connexion', {}, { context: contexte }).subscribe({ error: erreurRecue });
    serveur
      .expectOne('/api/auth/connexion')
      .flush({ detail: 'Identifiants incorrects' }, { status: 401, statusText: 'Unauthorized' });

    expect(erreurRecue).toHaveBeenCalled();
    expect(notifications.erreur()).toBe('Identifiants incorrects');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it("efface l'erreur précédente dès qu'une nouvelle action est envoyée", () => {
    notifications.signaler('Ancienne erreur');
    http.get('/api/clients').subscribe();
    expect(notifications.erreur()).toBe('Ancienne erreur');
    http.post('/api/clients', {}).subscribe();
    expect(notifications.erreur()).toBe('');
    serveur.match(() => true);
  });
});
