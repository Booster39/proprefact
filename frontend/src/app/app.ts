import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationStart,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { filter } from 'rxjs';
import { Session } from './auth/session';
import { Notifications } from './commun/notifications';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly session = inject(Session);
  protected readonly notifications = inject(Notifications);
  private readonly router = inject(Router);

  constructor() {
    // L'erreur d'un écran ne doit pas suivre l'utilisateur sur le suivant.
    this.router.events
      .pipe(
        filter((evenement) => evenement instanceof NavigationStart),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.notifications.effacer());
  }

  protected deconnexion(): void {
    this.session.fermer();
    this.router.navigateByUrl('/connexion');
  }
}
