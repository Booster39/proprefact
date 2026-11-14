import { Pipe, PipeTransform } from '@angular/core';

/** Date AAAA-MM-JJ reçue du back-end, affichée JJ/MM/AAAA ; un tiret si elle est absente. */
@Pipe({ name: 'dateFr' })
export class DateFrPipe implements PipeTransform {
  transform(date: string | null): string {
    return date ? date.split('-').reverse().join('/') : '—';
  }
}
