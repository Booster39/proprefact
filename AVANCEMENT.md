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
| 6 | Course entre requêtes (front) | Fait | voir journal |
| 7 | Règles métier dans `Facture`, exceptions métier | Fait | voir journal |
| 8 | Services clients et contrats, cycles entre paquets | Fait | voir journal |
| 9 | Duplications, e-mail hors transaction, configuration | Fait | voir journal |
| 10 | Découpage de `FacturXService` | Fait | voir journal |
| 11 | Migration V5 et compteur | Fait | voir journal |
| 12 | Front : découpage, typage, erreurs, double clic | Fait | voir journal |
| 13 | Front : connexion, styles, accessibilité, tests | Fait | voir journal |
| 14 | Projet : licences, README, divers | Fait | voir journal |
| 15 | Hors directives : règles d'architecture testées, garde-fous de build | Fait | voir journal |
| 16 | Hors directives : plus aucun setter dans les entités | Fait | voir journal |
| 17 | Hors directives : période typée `YearMonth` | Fait | voir journal |
| 18 | Hors directives : conditions du vendeur regroupées | Fait | voir journal |
| 19 | Hors directives : test d'intégration découpé par sujet | Fait | voir journal |
| 20 | Hors directives : capture d'écran refaite, boutons d'action visibles | Fait | voir journal |
| 21 | Hors directives : images Docker et lancement de production | Fait | voir journal |
| 22 | Hors directives : hébergement Railway, un seul service | En cours (étape 4/7 : base de données Railway) | voir journal |

États possibles : `À faire`, `En cours` (avec l'étape atteinte), `Fait`, `Bloqué : raison`.

## Résultats de référence

Après le dernier lot terminé, ces commandes donnent :

| Commande | Résultat |
|---|---|
| `cd backend && ./mvnw -B verify` (Docker lancé) | 53 tests, 0 échec, `BUILD SUCCESS`, contrôle Spotless réussi |
| `cd frontend && npm run format:check` | `All matched files use Prettier code style!` |
| `cd frontend && npm run lint` | `All files pass linting.` |
| `cd frontend && npm test` | 6 fichiers, 16 tests réussis |
| `cd frontend && npx ng build` | réussi |
| `./deploiement/construire.sh` puis `docker compose -f deploiement/docker-compose.yml up -d` | 3 conteneurs démarrés, parcours API à travers nginx réussi |
| intégration continue GitHub sur `main` (commit `0f9be46`) | 2 tâches réussies (`backend`, `frontend`) |
| parcours manuel dans le navigateur (voir ci-dessous) | 23 étapes réussies |

Si tu obtiens moins bien **avant** de commencer un lot, ne commence pas : cherche d'abord pourquoi.

## Ce qui reste à faire

Les 21 lots sont terminés. Ce qui suit n'a **pas** été fait ; chaque point
est un travail à part, à décider avec le propriétaire du projet avant de le commencer.

- **Hébergement.** Les images et le lancement sont prêts (lot 21), mais aucun hébergeur n'est
  choisi : machine, nom de domaine, HTTPS, serveur d'envoi d'e-mails, sauvegardes automatiques de
  la base. À décider avec le propriétaire.
- **Fonctions métier** listées dans le README, section « Reste à faire avant de vendre ».

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
- **Lot 6, test ajouté.** Le lot ne demandait pas de test ; `pages/clients.spec.ts` en ajoute un. Il
  sert de modèle pour tester un composant : `Api` remplacé par un objet simple, clics sur les
  vrais boutons, puis `await fixture.whenStable()` avant de lire la page.
- **Lot 7, méthodes rendues privées.** `Facture.figerParties` et `Facture.recalculerTotaux` sont
  maintenant `private` : plus aucun code extérieur ne les appelait. Pour modifier une facture
  depuis un service, ajoute une méthode métier dans `Facture`, jamais un setter.
- **Lot 7, ordre dans `valider`.** `FacturationService.valider` appelle `f.exiger(BROUILLON)`
  **avant** de prendre un numéro au compteur, puis `f.valider(...)` le revérifie. Ne retire pas le
  premier appel : sans lui, valider deux fois consommerait un numéro pour rien.
- **Lot 7, autres entités.** `Client`, `Site`, `Contrat`, `LigneFacture`, `Entreprise`,
  `Utilisateur` et `CompteurFacture` gardent `@Setter` : le lot ne visait que `Facture`.
- **Lot 8, exception dédiée.** La règle « la date de fin précède la date de début » lance
  `ValeurRefuseeException` (nouvelle, dans `domain`, traduite en 400) et non
  `IllegalArgumentException` comme l'écrivait le lot : intercepter toutes les
  `IllegalArgumentException` aurait renvoyé à l'utilisateur des messages techniques. Pour toute
  nouvelle règle métier sur une valeur saisie, lance `ValeurRefuseeException`.
- **Lot 8, objets gardés dans `web/Dtos.java`.** `ConfirmationReq` et `EmailReq` y restent : seul le
  contrôleur les utilise, les services reçoivent une simple chaîne.
- **Lot 8, `ouNull`.** `ClientService` a sa propre méthode privée `ouNull` ; `AuthService` a toujours
  `vide`. Le lot 9 (étape 9.2) les réunit dans `Textes`.
- **Règles de dépendances vérifiées par commande** (aucune ne doit rien afficher) :
  `grep -rn "import com.proprefact.web" backend/src/main/java/com/proprefact/service backend/src/main/java/com/proprefact/domain` ;
  `grep -rnE "import com.proprefact.repo|@Transactional" backend/src/main/java/com/proprefact/web` ;
  `grep -rn "import com.proprefact\\.\\(service\\|web\\|repo\\|config\\|tenant\\)" backend/src/main/java/com/proprefact/domain`.
- **Lot 9, paquet `proprietes`.** `ProprietesApp` est dans `com.proprefact.proprietes` et non dans
  `config` comme l'écrivait le lot : `service` en a besoin, et `config` dépend déjà de `service`.
  `proprietes` ne dépend d'aucun autre paquet du projet.
- **Lot 9, changement de comportement voulu.** Si le serveur de messagerie échoue, l'inscription
  répond quand même 202 et le compte est créé ; l'erreur est dans le journal et l'utilisateur
  redemande le lien. Avant, l'inscription échouait en 500 et rien n'était enregistré.
- **Lot 9, base de données.** Sans profil `dev`, l'application ne démarre plus si `DB_URL`,
  `DB_USER` ou `DB_PASSWORD` manque : c'est voulu (plus d'identifiants par défaut en production).
- **Vérifier un démarrage réel.** `docker compose up -d`, puis dans `backend/` :
  `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run`. Le journal doit contenir
  `Started ProprefactApplication`. À faire après tout lot qui touche à la configuration.
- **Lot 10, paquet.** `FacturXService` a lui aussi été déplacé dans `service/facturx/` (le lot le
  laissait dans `service/`) : ainsi `FactureXml`, `FacturePdf` et `Mentions` restent à visibilité de
  paquet.
- **Lot 10, délai de paiement.** La mention « Paiement à 30 jours » se calcule maintenant sur les
  dates de la facture (échéance moins émission) au lieu de lire la constante
  `FacturationService.DELAI_PAIEMENT_JOURS` : même texte, et plus de dépendance vers `service`.
- **Méthode pour un remaniement sans changement de résultat** (utilisée au lot 10) : avant de
  toucher au code, écrire un test provisoire qui enregistre le résultat dans un fichier ; refaire
  tourner après ; comparer avec `diff` ; supprimer le test provisoire.
- **Lot 11, nouvelle valeur de statut.** La contrainte `facture_statut_connu` liste les valeurs de
  `StatutFacture`. Ajouter une valeur à l'énumération exige une migration qui remplace la
  contrainte, sinon l'enregistrement échoue.
- **Lots 12 et 13, un seul commit.** Les deux lots ont été faits ensemble (les mêmes gabarits
  étaient réécrits) et sont dans un seul commit, contrairement à « un commit par étape ».
- **Lot 12, formulaires réactifs.** L'étape 12.6 proposait de garder `ngModel` avec des signaux.
  Les écrans utilisent à la place des **formulaires réactifs typés** (`NonNullableFormBuilder`,
  `formControlName`) : la validation est dans le fichier `.ts`, l'objet envoyé est typé, et
  `form.reset()` remet tout à zéro. Tout nouveau formulaire doit suivre ce modèle ; `FormsModule`
  et `ngModel` ne sont plus utilisés nulle part.
- **Lot 12, erreurs centralisées.** L'intercepteur (`auth/intercepteur-jeton.ts`) affiche toute
  erreur du back-end par `commun/notifications.ts`, et le composant racine la montre en haut de la
  page. Un écran n'écrit donc **pas** de `error:` dans ses `subscribe` : en cas d'échec sa requête
  se termine sans résultat. Pour remettre un indicateur à zéro, utiliser `finalize`. Seuls
  `Api.connexion` et `Api.confirmer` reçoivent l'erreur (jeton de contexte `ERREUR_TRANSMISE`),
  parce que leur écran réagit à l'échec.
- **Lot 12, défaut trouvé par le parcours navigateur.** Sans cette terminaison silencieuse, chaque
  erreur HTTP apparaissait dans la console comme « ERROR HttpErrorResponse » non gérée. Les tests
  unitaires ne le montraient pas : seul le parcours réel l'a révélé.
- **Lot 13, écran d'inscription.** Après la création du compte, l'écran d'inscription affiche
  lui-même le message « e-mail envoyé » et le bouton de renvoi (composant
  `pages/renvoi-confirmation.ts`, partagé avec la connexion).
- **Parcours manuel dans le navigateur.** À refaire après tout changement d'écran. Démarrer
  `docker compose up -d`, le back-end en profil `dev`, puis `npm start` dans `frontend/`. Dérouler :
  inscription avec un SIREN faux (bouton grisé, bordure rouge) puis juste ; connexion avant
  confirmation (erreur + renvoi proposé) ; lien de confirmation lu dans le journal du back-end ;
  2 clients avec un site chacun ; contrat (changer de client vide bien la liste des sites) ;
  résiliation avec une date avant le début (refus affiché) ; génération du mois ; validation
  (confirmation demandée) ; téléchargement du Factur-X ; dépôt ; paiement ; facture ponctuelle
  créée puis supprimée. La console du navigateur ne doit montrer aucune erreur JavaScript.
- **Parcours navigateur, limite de tentatives.** Rejoué de nombreuses fois de suite, le parcours
  finit par échouer à l'inscription ou à la connexion : le back-end répond 429 « Trop de
  tentatives » (30 appels d'authentification par adresse IP et par quart d'heure). Ce n'est pas un
  défaut. Redémarrer le back-end remet les compteurs à zéro.
- **Lot 15, ajouté après les directives.** Il traite ce que la revue signalait et que les 14 lots
  ne couvraient pas : `ArchitectureTest` (ArchUnit) vérifie à chaque build les règles de
  dépendances entre paquets ; `maven-enforcer` exige Java 21 et Maven 3.9 ; JaCoCo écrit le rapport
  de couverture dans `backend/target/site/jacoco/index.html` (94 % des instructions au
  2026-11-16). Le test a trouvé trois `ResponseStatusException` restantes dans `AuthService`,
  remplacées par `IdentifiantsIncorrectsException` (401), `AdresseNonConfirmeeException` (403) et
  `ValeurRefuseeException` (400), mêmes messages. Seul `AuthController` lance encore une
  `ResponseStatusException` (429, limite de tentatives) : c'est permis, il est dans `web`.
- **Si `ArchitectureTest` échoue**, ne modifie pas le test : déplace la classe fautive ou inverse
  la dépendance. Le message d'échec nomme la classe et la ligne.
- **Lot 16, entités sans setters.** Aucune entité n'a plus `@Setter`. Pour créer ou modifier :
  `new Client(fiche)` et `client.mettreAJour(fiche)` avec un `FicheClient` ; de même `Entreprise`
  avec `FicheEntreprise` ; `new Site(client, nom, adresse, codePostal, ville)` ;
  `new Contrat(site, libelle, montant, taux, debut)` puis `terminerLe(fin)` ;
  `LigneFacture.ponctuelle(...)` ou `LigneFacture.mensualite(contrat, periode, designation)` ;
  `compteur.suivant()` ; `new Utilisateur(email, hash, entreprise)`, `remplacerInscription`,
  `attendreConfirmation`, `lienValableA`, `confirmerEmail`. Une règle de `ArchitectureTest`
  interdit toute méthode `set…` dans `domain` : pour un nouveau besoin, écrire une méthode métier.
- **Lot 16, objets de test.** `backend/src/test/java/com/proprefact/domain/Exemples.java` fournit
  un client, un vendeur, des lignes, un brouillon et un contrat prêts à l'emploi : s'en servir dans
  tout nouveau test unitaire au lieu de reconstruire ces objets.
- **Lot 16, `AuthService.inscrire`.** Le déroulement est inchangé (même ordre, mêmes réponses) ;
  seule l'écriture change. Les tests d'inscription, de réinscription et de remplacement d'une
  inscription non confirmée passent sans modification.
- **Lot 17, période.** `Facture.periode` et `LigneFacture.periode` sont des `YearMonth`, enregistrés
  « AAAA-MM » par `domain/MoisConverter` (appliqué automatiquement, aussi aux paramètres de
  requête). Le schéma ne change pas. Le JSON garde la chaîne « 2026-11 » : `FactureDto` la convertit
  explicitement, et `FacturationTest` le vérifie.
- **Lot 18, conditions du vendeur.** `Facture.getConditionsVendeur()` (embeddable
  `ConditionsVendeur`) remplace `getVendeurIban()`, `getVendeurBic()` et
  `getVendeurOptionTvaDebits()`. Mêmes colonnes en base (`vendeur_iban`, `vendeur_bic`,
  `vendeur_option_tva_debits`), aucune migration. `isOptionTvaDebits()` renvoie `false` quand la
  colonne est vide. `FacturXServiceTest` vérifie désormais l'IBAN et le code de TVA dans le XML.
- **Lot 19, tests d'intégration.** `ParcoursFacturationTest` n'existe plus. Ses 11 tests sont dans
  `ComptesTest`, `FacturationTest`, `DepotTest` et `CloisonnementTest`, qui héritent de
  `TestIntegration` (annotations, `mvc`, `courrier`, méthodes `inscrire`, `envoyer`, `id`…). Les
  doubles `Courrier` et `PlateformeDeTest` sont des classes à part, déclarées par `DoublesDeTest`.
  Toutes les classes partagent un seul contexte Spring et un seul conteneur PostgreSQL ; chacune
  passe aussi lancée seule (vérifié). Un nouveau test d'intégration : `class XxxTest extends
  TestIntegration`, sans annotation, et une entreprise propre au test (e-mail et SIREN uniques).
- **Piège rencontré au lot 19.** Un script de découpage a échoué à mi-chemin, et la commande
  suivante, enchaînée avec `;`, a supprimé le fichier d'origine : la vérification n'a alors compté
  que 27 tests au lieu de 38. Rien n'a été commité. Règle : enchaîne avec `&&` toute commande qui
  supprime quelque chose, et compare toujours le nombre de tests à celui du tableau ci-dessus.
- **Lot 20, défaut d'affichage trouvé par la capture.** Sur un écran de 1200 points, la colonne des
  actions de la liste des factures débordait : « Déposer », « Payée » et « Supprimer » n'étaient
  visibles qu'en faisant défiler le tableau. Les boutons passent maintenant à la ligne dans leur
  cellule (`td.actions-ligne`) et la page s'élargit à 1200 points. En dessous d'environ 1100
  points (tablette, téléphone), le tableau défile toujours horizontalement : c'est voulu pour
  l'instant, une vraie version mobile (cartes au lieu du tableau) reste à décider.
- **Refaire la capture du README.** Lancer base, back-end et front comme pour le parcours manuel,
  créer quatre clients avec un contrat chacun, générer novembre, mettre les factures dans des
  statuts différents, puis capturer `/factures` à 1200 × 800 points, échelle 2. Le sélecteur de
  mois s'affiche en anglais dans le Chromium de capture (langue du navigateur, pas de la page) :
  ce n'est pas un défaut de l'application.
- **Fusion dans `main`.** Ne fusionne dans `main` que sur demande explicite du propriétaire, pour
  chaque lot ou groupe de lots : un accord donné une fois ne vaut pas pour les lots suivants.
- **Lot 21, déploiement.** `backend/Dockerfile` et `frontend/Dockerfile` copient des artefacts déjà
  construits (jar, `dist/`) : les conteneurs de l'environnement de travail n'ont pas d'accès réseau,
  une image qui télécharge ses dépendances n'y serait pas vérifiable. `deploiement/construire.sh`
  enchaîne tests, compilation et images. Les images se construisent sans secret ; les secrets ne
  servent qu'au lancement (`deploiement/.env`, ignoré par git). Le projet Docker s'appelle
  `proprefact-production` pour ne pas partager le volume de la base de développement.
- **Lot 21, deux défauts trouvés en lançant réellement la pile.** (1) nginx complétait l'en-tête
  `X-Forwarded-For` envoyé par le client : en inventant une adresse à chaque requête, on
  contournait la limite de tentatives de connexion (35 essais passés, vérifié). nginx remplace
  maintenant l'en-tête par l'adresse réelle : blocage à la 31e tentative (vérifié). (2) Après
  recréation du back-end (nouvelle adresse interne), nginx renvoyait 502 jusqu'à son redémarrage ;
  il résout maintenant le nom `backend` par le DNS de Docker (vérifié). Ne pas revenir sur ces deux
  réglages de `frontend/nginx.conf` sans relire leurs commentaires.
- **Lot 22, front servi par Spring Boot.** Le pom ajoute `../frontend/dist/frontend/browser` aux ressources
  (`static/` dans le jar) : lancer `npx ng build` dans `frontend/` **avant** `./mvnw package`, sinon le jar ne
  contient que l'API (Maven ignore le dossier absent sans erreur). `config/FrontConfig` renvoie `index.html` pour
  toute adresse sans extension hors `/api` et `/actuator` ; les `*.js`, `*.css` et `media/` (noms hachés) sont en
  cache un an, le reste en `no-cache`. Les tests utilisent `src/test/resources/static/` (un `index.html` et un
  `.js` de test), placés avant le vrai front dans le classpath de test.
- **Lot 22, piège de la racine.** `/` n'est pas servi par `FrontConfig` mais par la « page d'accueil » de Spring
  Boot, qui fait un renvoi interne vers `index.html`. MockMvc n'exécute pas ce renvoi (pas de contenu, pas de
  type) : `FrontTest` vérifie donc `forwardedUrl("index.html")` pour `/`. En réel, `/` renvoie bien la page
  (vérifié avec `curl`).
- **Lot 22, Dockerfile de la racine.** Trois étapes : Node (`npm ci`, `npx ng build`), Maven
  (`-Dmaven.test.skip=true package`, le front compilé copié dans `../frontend/dist/frontend/browser`), image
  finale `eclipse-temurin:21-jre`, utilisateur `proprefact` (uid 10001). Le port vient de `PORT`
  (`server.port=${PORT:8080}`). Les conteneurs de l'environnement de travail n'ont pas de réseau : les deux
  premières étapes ne s'y construisent pas. Pour vérifier localement, reproduire les étapes hors Docker sur une
  copie propre, mettre le jar dans `<dossier>/src/app.jar`, puis
  `docker buildx build --load --build-context back=<dossier> -t proprefact .` : le contexte nommé `back`
  remplace l'étape Maven et seule l'étape finale du vrai Dockerfile est construite. La construction complète
  n'est prouvée que par la tâche `image` de l'intégration continue.
- **Lot 22, adresse IP derrière Railway.** `web/AdresseClient` lit l'adresse du client dans UN en-tête choisi
  (`ENTETE_IP_CLIENT`, propriété `app.auth.entete-ip-client`) : `X-Real-IP` sur Railway. Valeur absente ou
  qui n'est pas une adresse (liste, texte) : adresse de la connexion. `X-Forwarded-For` n'est plus jamais lu et
  `server.forward-headers-strategy=none` est fixé (la variable `FORWARD_HEADERS` n'existe plus) : en mode
  `framework`, Spring prend la première adresse de `X-Forwarded-For`, que le client peut inventer si le proxy
  complète l'en-tête. Source : la page officielle « Public Networking — Specs & Limits » de Railway liste
  `X-Real-IP` (adresse du client) parmi les en-têtes posés par le proxy ; qu'il **écrase** une valeur envoyée
  par le client n'est affirmé que par le personnel de Railway sur leur forum (Central Station), pas dans la doc.
  Incertitude écrite dans `DEPLOIEMENT-RAILWAY.md`. `docs.railway.com` n'était pas joignable depuis
  l'environnement de travail : informations obtenues par recherche web.
- **Lot 22, `AdresseIpTest`.** Seule classe d'intégration avec ses propres réglages (`@TestPropertySource` :
  limite de 3, en-tête `X-Real-IP`) : elle a son propre contexte Spring et sa propre base PostgreSQL, ce qui
  ajoute environ 8 secondes. Elle échoue si l'on relit `X-Forwarded-For` (vérifié en simulant le défaut).
- **Lot 22, base Railway.** `spring.datasource.*` lit `DB_URL`, `DB_USER`, `DB_PASSWORD`, et à défaut les
  variables standard `PGHOST`, `PGPORT` (5432 par défaut), `PGDATABASE`, `PGUSER`, `PGPASSWORD` que fournit le
  service PostgreSQL de Railway. Raison : sur Railway il suffit de cinq références `${{Postgres.PGHOST}}`…, et
  le format `.railway/railway.ts` ne sait pas composer une chaîne (`jdbc:postgresql://…`) à partir de
  références (vérifié dans le SDK `railway` 3.17.0 : une référence est un objet, pas un texte). Sans aucune de
  ces variables, le démarrage échoue toujours (règle du lot 9). `ConnexionBaseTest` (3 tests) le vérifie.
- **Vérifier un déploiement.** Un fichier de réglages jetable hors du dépôt
  (`docker compose … --env-file chemin/vers/essai.env`), `PORT_HTTP` libre, puis : accueil 200,
  `/factures` renvoie la page de l'application, `/api/clients` 401, inscription 202, lien de
  confirmation dans `docker compose … logs backend`, facture validée, Factur-X téléchargé.
  Arrêter avec `down -v` (supprime le volume de la pile d'essai).

## Journal

| Date | Lot | Résumé |
|---|---|---|
| 2026-11-14 | 1 | Maven Wrapper, Spotless (42 fichiers Java reformatés), Prettier (16 fichiers), ESLint, cible de tests Vitest avec 2 fichiers de tests, `.editorconfig`, `.gitattributes`, intégration continue GitHub, README. |
| 2026-11-14 | 2 | Tailles et décimales validées sur tous les objets reçus, messages en français, `ErreursApi` renvoie une propriété `erreurs` par champ et ne laisse plus passer d'erreur 500 sur une contrainte de base, nouveau test `lesDonneesInvalidesSontRefuseesChampParChamp`. |
| 2026-11-14 | 3 | `messageErreur` détaille les erreurs par champ avec leur libellé, bordure rouge sur les champs invalides touchés, phrase d'aide au-dessus des boutons d'envoi (clients, sites, contrats, facture ponctuelle, connexion), un test ajouté. |
| 2026-11-14 | 4 | Bean `Clock` en Europe/Paris (`config/HorlogeConfig`) injecté dans `FacturationService`, `AuthService`, `DepotService`, `RelanceDepots` et `LimiteurTentatives` ; plus aucun `now()` sans horloge dans le code principal ; côté front, fonction `aujourdhui()` en heure locale. |
| 2026-11-14 | 5 | Tests d'intégration sur PostgreSQL 16 via Testcontainers (`BaseDeTest`), H2 retiré, profil `test` qui ne contient plus que trois écarts : la configuration de production est désormais réellement chargée par les tests. |
| 2026-11-14 | 6 | `switchMap` sur le chargement des sites dans `Clients` et `Contrats` ; test `clients.spec.ts` qui reproduit la course (il échoue sans la correction, vérifié). |
| 2026-11-14 | 7 | `Facture` n'a plus de setters : création par `Facture.brouillon`, transitions `valider`, `demanderDepot`, `enregistrerDepotReussi`, `enregistrerDepotEchoue`, `marquerPayee`, `exiger` ; `tvaParTaux()` sert aux totaux et au PDF ; exceptions `TransitionInterditeException` (409) et `IntrouvableException` (404) ; tests `FactureTest`, `ContratTest`, `DepotServiceTest`. |
| 2026-11-14 | 8 | Objets reçus déplacés dans `service.commande` (`…Commande`, `JetonConnexion`, `Messages`) ; nouveaux `ClientService` et `ContratService` ; contrôleurs sans dépôt ni `@Transactional` ; `FichierFacture` ; `RelanceDepots` dans `service`, `TenantResolver` dans `tenant`, `@EnableScheduling` sur l'application ; règle de date de fin dans `Contrat.terminerLe` ; 2 tests ajoutés. |
| 2026-11-14 | 9 | `Empreinte` et `Textes` remplacent les copies ; les e-mails partent après validation de la transaction (`EmailAEnvoyer`, `EnvoiEmails`) ; réglages `app.*` regroupés et validés dans `proprietes/ProprietesApp` ; identifiants de base par défaut déplacés dans le profil `dev`. Démarrage réel vérifié en profil `dev` contre PostgreSQL (4 migrations appliquées, inscription 202, lien de confirmation dans le journal). |
| 2026-11-14 | 10 | `FacturXService` éclaté dans `service/facturx/` : `FactureXml` (données Mustang), `FacturePdf` (mise en page), `Mentions`, et un `FacturXService` de 42 lignes ; variables renommées, coordonnées nommées, caractères invisibles remplacés par leurs codes, police absente signalée clairement, PDFBox et XMPBox déclarés ; test `FacturXServiceTest` (3 tests). Fichier produit comparé avant et après sur une facture à 3 taux : XML, texte et flux de dessin de la page identiques. |
| 2026-11-14 | 11 | Migration `V5__index_et_contraintes.sql` (5 index, contrainte sur le statut, unicité de l'e-mail sans casse), appliquée sur PostgreSQL ; `CompteurFactureRepository.creerSiAbsent` avant le verrou ; test `desValidationsSimultaneesNumerotentSansDoublonNiTrou` (6 validations en parallèle), qui échoue avec l'ancien code (clé dupliquée, vérifié). |
| 2026-11-14 | 12 | Front découpé en `auth/`, `api/`, `commun/`, `pages/` ; pipes `montant` et `dateFr` ; objets envoyés typés (plus aucun `object`) ; `ACTIONS_PAR_STATUT` et `CLASSE_STATUT` ; formulaires réactifs typés ; erreurs affichées par l'intercepteur ; protection contre le double clic (`enCours`) ; confirmation avant valider, payer, supprimer. |
| 2026-11-14 | 13 | Écrans `connexion` et `inscription` séparés, titre par route, plus aucun style en ligne, tableaux avec légende et `scope`, boutons typés ; tests de l'intercepteur, de la garde et de l'écran des factures. Parcours complet rejoué dans un navigateur contre le vrai back-end : 23 étapes réussies. |
| 2026-11-14 | 14 | Licence des polices DejaVu jointe (et présente dans le jar), README : capture d'écran, exemple de facture, organisation du front, section « Conventions » ; `.gitignore` complété ; paquet npm renommé `proprefact-frontend` 0.1.0. |
| 2026-11-16 | 15 | `ArchitectureTest` (5 règles), exceptions métier dans `AuthService`, `maven-enforcer`, rapport de couverture JaCoCo. |
| 2026-11-16 | 16 | `@Setter` retiré de `Client`, `Site`, `Contrat`, `LigneFacture`, `Entreprise`, `Utilisateur`, `CompteurFacture` ; constructeurs et méthodes métier à la place ; `FicheClient`, `FicheEntreprise` ; règle d'architecture « aucun setter dans le domaine » ; `Exemples` et `UtilisateurTest`. |
| 2026-11-16 | 17 | `periode` typée `YearMonth` avec `MoisConverter` ; plus aucun `YearMonth.parse` dans le code ; JSON inchangé et vérifié. |
| 2026-11-16 | 18 | `ConditionsVendeur` regroupe IBAN, BIC et option pour les débits ; test du XML complété. |
| 2026-11-16 | 19 | `ParcoursFacturationTest` découpé en `ComptesTest`, `FacturationTest`, `DepotTest`, `CloisonnementTest` sur une base `TestIntegration` ; même nombre de tests (38). |
| 2026-11-16 | 20 | Capture `docs/ecran-factures.png` refaite (5 factures, tous les statuts) ; boutons d'action qui passent à la ligne au lieu de déborder ; page élargie à 1200 points. |
| 2026-11-19 | 22 (1/7) | Front Angular compilé embarqué dans le jar et servi par Spring Boot (`FrontConfig`) : adresses de l'application renvoyées vers `index.html`, cache long pour les fichiers hachés, `no-cache` pour `index.html`, en-têtes nosniff, X-Frame-Options et Referrer-Policy, compression ; `/api/**` reste protégé ; `FrontTest` (6 tests). Vérifié aussi avec le vrai jar et `curl`. |
| 2026-11-19 | 22 (2/7) | `Dockerfile` et `.dockerignore` à la racine (Node, Maven, JRE, non root, `MaxRAMPercentage=75`), port lu dans `PORT` ; tâche `image` dans l'intégration continue, qui tourne aussi sur la branche `railway`. Étape finale construite localement avec le jar reproduit hors Docker, image lancée avec `PORT=9090` (`/factures` 200, uid 10001). |
| 2026-11-19 | 22 (3/7) | Limite par IP sur l'adresse écrite par le proxy de Railway (`X-Real-IP`, réglage `ENTETE_IP_CLIENT`), jamais sur `X-Forwarded-For` ; `FORWARD_HEADERS` supprimé ; `AdresseIpTest` (3 tests) et `AdresseClientTest` (3 tests). |
| 2026-11-19 | 22 (4/7) | Connexion à la base par les variables `PG*` de Railway quand `DB_*` manquent ; `ConnexionBaseTest` (3 tests). |
| 2026-11-19 | 21 | Dockerfiles back et front, `nginx.conf`, `deploiement/docker-compose.yml`, `.env.exemple`, `construire.sh` ; images construites en intégration continue ; deux défauts de nginx corrigés (contournement de la limite par IP, back-end recréé injoignable). |
