/** Date du jour dans le fuseau du navigateur, au format AAAA-MM-JJ (toISOString donnerait la date UTC). */
export function aujourdhui(): string {
  const d = new Date();
  const deuxChiffres = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${deuxChiffres(d.getMonth() + 1)}-${deuxChiffres(d.getDate())}`;
}
