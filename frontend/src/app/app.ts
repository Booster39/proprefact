import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Session } from './session';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly session = inject(Session);
  private readonly router = inject(Router);

  protected deconnexion(): void {
    this.session.fermer();
    this.router.navigateByUrl('/connexion');
  }
}
