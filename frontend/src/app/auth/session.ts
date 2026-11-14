import { Injectable, computed, signal } from '@angular/core';
import { Jeton } from '../api/modeles';

const CLE = 'proprefact.session';

/** Session de l'utilisateur connecté, conservée dans le navigateur. */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly etat = signal<Jeton | null>(this.lire());
  readonly connecte = computed(() => this.etat() !== null);
  readonly raisonSociale = computed(() => this.etat()?.raisonSociale ?? '');

  jeton(): string | null {
    return this.etat()?.jeton ?? null;
  }

  ouvrir(j: Jeton): void {
    this.etat.set(j);
    try {
      localStorage.setItem(CLE, JSON.stringify(j));
    } catch {
      /* stockage indisponible */
    }
  }

  fermer(): void {
    this.etat.set(null);
    try {
      localStorage.removeItem(CLE);
    } catch {
      /* stockage indisponible */
    }
  }

  private lire(): Jeton | null {
    try {
      const brut = localStorage.getItem(CLE);
      return brut ? (JSON.parse(brut) as Jeton) : null;
    } catch {
      return null;
    }
  }
}
