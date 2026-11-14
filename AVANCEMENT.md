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
| 4 | Horloge injectable et dates locales | Fait | voir journal |
| 5 | Tests sur PostgreSQL et profil de test | Fait | voir journal |
| 6 | Course entre requêtes (front) | À faire | |
| 7 | Règles métier dans `Facture`, exceptions métier | À faire | |
| 8 | Services clients et contrats, cycles entre paquets | À faire | |
| 9 | Duplications, e-mail hors transaction, configuration | À faire | |
| 10 | Découpage de `FacturXService` | À faire | |
| 11 | Migration V5 et compteur | À faire | |
| 12 | Front : découpage, typage, erreurs, double clic | À faire | |
| 13 | Front : connexion, styles, accessibilité, tests | À faire | |
| 14 | Projet : licences, README, divers | À faire | |

États possibles : `À faire`, `En cours` (avec l'étape atteinte), `Fait`, `Bloqué : raison`.

## Résultats de référence

Après le dernier lot terminé, ces commandes donnent :

| Commande | Résultat |
|---|---|
| `cd backend && ./mvnw -B verify` (Docker lancé) | 11 tests, 0 échec, `BUILD SUCCESS`, contrôle Spotless réussi |
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
- **Docker.** Dans l'environnement utilisé, le démon Docker n'est pas lancé au départ : `docker info`
  échoue. Il suffit de le démarrer : `nohup dockerd > /tmp/dockerd.log 2>&1 &`, puis d'attendre que
  `docker info` affiche `Server Version`. À refaire à chaque nouvelle session, **avant**
  `./mvnw verify`, sinon les tests d'intégration échouent avec `Could not find a valid Docker
  environment`.
- **Lot 5, piège du dossier `target/`.** Après la suppression de
  `src/test/resources/application.properties`, l'ancienne copie restait dans `target/test-classes/`
  et les tests passaient pour de mauvaises raisons. Règle : après avoir supprimé ou renommé un
  fichier de `src/`, lance `./mvnw -B clean verify` (avec `clean`).
- **Lot 5, résultat.** Les 11 tests passent sur PostgreSQL 16 sans aucune modification du code
  principal ni des migrations : H2 ne cachait pas de défaut.

## Journal

| Date | Lot | Résumé |
|---|---|---|
| 2026-11-14 | 1 | Maven Wrapper, Spotless (42 fichiers Java reformatés), Prettier (16 fichiers), ESLint, cible de tests Vitest avec 2 fichiers de tests, `.editorconfig`, `.gitattributes`, intégration continue GitHub, README. |
| 2026-11-14 | 2 | Tailles et décimales validées sur tous les objets reçus, messages en français, `ErreursApi` renvoie une propriété `erreurs` par champ et ne laisse plus passer d'erreur 500 sur une contrainte de base, nouveau test `lesDonneesInvalidesSontRefuseesChampParChamp`. |
| 2026-11-14 | 3 | `messageErreur` détaille les erreurs par champ avec leur libellé, bordure rouge sur les champs invalides touchés, phrase d'aide au-dessus des boutons d'envoi (clients, sites, contrats, facture ponctuelle, connexion), un test ajouté. |
| 2026-11-14 | 4 | Bean `Clock` en Europe/Paris (`config/HorlogeConfig`) injecté dans `FacturationService`, `AuthService`, `DepotService`, `RelanceDepots` et `LimiteurTentatives` ; plus aucun `now()` sans horloge dans le code principal ; côté front, fonction `aujourdhui()` en heure locale. |
| 2026-11-14 | 5 | Tests d'intégration sur PostgreSQL 16 via Testcontainers (`BaseDeTest`), H2 retiré, profil `test` qui ne contient plus que trois écarts : la configuration de production est désormais réellement chargée par les tests. |
