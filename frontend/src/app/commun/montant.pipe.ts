import { Pipe, PipeTransform } from '@angular/core';

const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });

/** Montant en euros au format français : 1 234,50 €. */
@Pipe({ name: 'montant' })
export class MontantPipe implements PipeTransform {
  transform(valeur: number): string {
    return euros.format(valeur);
  }
}
