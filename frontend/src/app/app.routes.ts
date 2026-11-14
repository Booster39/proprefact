import { Routes } from '@angular/router';
import { connexionRequise } from './auth/connexion-requise';

export const routes: Routes = [
  {
    path: 'connexion',
    title: 'Connexion – ProPreFact',
    loadComponent: () => import('./pages/connexion').then((m) => m.Connexion),
  },
  {
    path: 'inscription',
    title: 'Créer un compte – ProPreFact',
    loadComponent: () => import('./pages/inscription').then((m) => m.Inscription),
  },
  {
    path: 'confirmation',
    title: 'Confirmation – ProPreFact',
    loadComponent: () => import('./pages/confirmation').then((m) => m.Confirmation),
  },
  {
    path: 'factures',
    title: 'Factures – ProPreFact',
    canActivate: [connexionRequise],
    loadComponent: () => import('./pages/factures').then((m) => m.Factures),
  },
  {
    path: 'contrats',
    title: 'Contrats – ProPreFact',
    canActivate: [connexionRequise],
    loadComponent: () => import('./pages/contrats').then((m) => m.Contrats),
  },
  {
    path: 'clients',
    title: 'Clients et sites – ProPreFact',
    canActivate: [connexionRequise],
    loadComponent: () => import('./pages/clients').then((m) => m.Clients),
  },
  { path: '**', redirectTo: 'factures' },
];
