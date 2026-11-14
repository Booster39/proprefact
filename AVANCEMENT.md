# Avancement des corrections

Ce fichier dit où en est l'application de `DIRECTIVES_CORRECTIONS.md`. Il est mis à jour et commité
**dans le même commit que chaque lot**.

## Pour reprendre le travail

1. Lis `DIRECTIVES_CORRECTIONS.md`, sections 1 à 4, en entier.
2. Travaille sur la branche `claude/directives-corrections` (`git switch claude/directives-corrections`).
3. Dans le tableau ci-dessous, prends le **premier lot dont l'état est « À faire »** et dont les
   dépendances sont « Fait ». Ne saute pas de lot sans raison écrite ici.
4. Applique le lot, lance ses vérifications, puis mets à jour ce fichier : état du lot, ligne dans
   « Journal », et tout écart dans « Écarts et pièges rencontrés ».
5. Commite le lot **avec** ce fichier, puis pousse : `git push -u origin claude/directives-corrections`.

## État des lots

| Lot | Sujet | État | Commit |
|---|---|---|---|
| 1 | Outillage | Fait | voir journal |
| 2 | Validation et erreurs lisibles (back) | Fait | voir journal |
| 3 | Affichage des erreurs de validation (front) | Fait | voir journal |
| 4 | Horloge injectable et dates locales | À faire | |
| 5 | Tests sur PostgreSQL et profil de test | Bloqué : pas de Docker dans l'environnement utilisé | |
| 6 | Course entre requêtes (front) | À faire | |
| 7 | Règles métier dans `Facture`, exceptions métier | À faire | |
| 8 | Services clients et contrats, cycles entre paquets | À faire | |
| 9 | Duplications, e-mail hors transaction, configuration | À faire | |
| 10 | Découpage de `FacturXService` | À faire | |
| 11 | Migration V5 et compteur | Bloqué : dépend du lot 5 | |
| 12 | Front : découpage, typage, erreurs, double clic | À faire | |
| 13 | Front : connexion, styles, accessibilité, tests | À faire | |
| 14 | Projet : licences, README, divers | À faire | |

États possibles : `À faire`, `En cours` (avec l'étape atteinte), `Fait`, `Bloqué : raison`.

## Résultats de référence

Après le dernier lot terminé, ces commandes donnent :

| Commande | Résultat |
|---|---|
| `cd backend && ./mvnw -B verify` | 11 tests, 0 échec, `BUILD SUCCESS`, contrôle Spotless réussi |
| `cd frontend && npm run format:check` | `All matched files use Prettier code style!` |
| `cd frontend && npm run lint` | `All files pass linting.` |
| `cd frontend && npm test` | 2 fichiers, 4 tests réussis |
| `cd frontend && npx ng build` | réussi |

Si tu obtiens moins bien **avant** de commencer un lot, ne commence pas : cherche d'abord pourquoi.

## Écarts et pièges rencontrés

- **Lot 1, Prettier.** La première exécution de `npm run format` n'a pas suffi pour
  `pages/clients.ts` et `pages/contrats.ts` : `format:check` échouait encore. Une seconde exécution
  de `npm run format` a réglé le problème. Règle : après `npm run format`, lance toujours
  `npm run format:check` ; s'il échoue, relance `npm run format` une fois.
- **Lot 1, `angular.json`.** Le fichier a été réécrit entièrement (indentation normalisée) en plus
  des deux changements demandés (suppression de `schematics`, ajout de la cible `test`). Aucun
  autre réglage n'a changé.
- **Maven.** Dans cet environnement, les téléchargements Maven échouent souvent au premier essai
  (`Could not find artifact`). La boucle avec `-U` de la section 2 des directives règle le problème.
- **Docker.** Le démon Docker ne tourne pas dans l'environnement utilisé jusqu'ici : les lots 5 et
  11 n'y sont pas faisables. Ils sont à faire sur un poste où `docker info` affiche
  `Server Version`.
- **Lot 2, messages de validation.** Les messages (« obligatoire », « 200 caractères au plus »…)
  sont des constantes en tête de `Dtos.java` (`OBLIGATOIRE`, `MAX_200`…) au lieu d'être répétés
  dans chaque annotation comme l'écrit le lot. Le résultat est identique. Pour un nouveau champ,
  réutilise ces constantes.
- **Lot 2, formatage.** Spotless place une ligne vide entre les champs d'un `record` dès qu'un
  champ porte plusieurs annotations : c'est normal, ne le corrige pas à la main.
- **Lot 3, vérification manuelle.** Faute de base PostgreSQL (pas de Docker), l'écran n'a pas été
  essayé dans un navigateur. Seuls le build, le lint et les tests unitaires ont tourné. À vérifier
  à la main : saisir un SIREN de 8 chiffres, quitter le champ, voir la bordure rouge et la phrase
  d'aide.
- **Lot 3, écran de connexion.** La phrase d'aide a aussi été ajoutée à `connexion.html`, que le
  lot ne citait pas.

## Journal

| Date | Lot | Résumé |
|---|---|---|
| 2026-11-14 | 1 | Maven Wrapper, Spotless (42 fichiers Java reformatés), Prettier (16 fichiers), ESLint, cible de tests Vitest avec 2 fichiers de tests, `.editorconfig`, `.gitattributes`, intégration continue GitHub, README. |
| 2026-11-14 | 2 | Tailles et décimales validées sur tous les objets reçus, messages en français, `ErreursApi` renvoie une propriété `erreurs` par champ et ne laisse plus passer d'erreur 500 sur une contrainte de base, nouveau test `lesDonneesInvalidesSontRefuseesChampParChamp`. |
| 2026-11-14 | 3 | `messageErreur` détaille les erreurs par champ avec leur libellé, bordure rouge sur les champs invalides touchés, phrase d'aide au-dessus des boutons d'envoi (clients, sites, contrats, facture ponctuelle, connexion), un test ajouté. |
