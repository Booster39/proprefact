import { Routes } from '@angular/router';
import { connexionRequise } from './api';

export const routes: Routes = [
  { path: 'connexion', loadComponent: () => import('./pages/connexion').then((m) => m.Connexion) },
  { path: 'factures', canActivate: [connexionRequise], loadComponent: () => import('./pages/factures').then((m) => m.Factures) },
  { path: 'contrats', canActivate: [connexionRequise], loadComponent: () => import('./pages/contrats').then((m) => m.Contrats) },
  { path: 'clients', canActivate: [connexionRequise], loadComponent: () => import('./pages/clients').then((m) => m.Clients) },
  { path: '**', redirectTo: 'factures' },
];
