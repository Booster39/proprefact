# Mettre ProPreFact en ligne sur Railway

Ce guide s'adresse à quelqu'un qui n'a jamais utilisé Railway. Suivez les étapes dans l'ordre ; chacune dit quoi
vérifier avant de passer à la suivante.

**À savoir avant de commencer.** Ce guide a été écrit le 19 novembre 2026 à partir de la documentation publique de
Railway, **sans compte Railway** : rien n'a été déployé pour de vrai. Les noms de menus peuvent légèrement
différer de ce que vous verrez ; ce qui compte, ce sont les valeurs à saisir. Les points incertains sont signalés
par « ⚠ ». Ce qui a réellement été vérifié est listé à la fin (« Ce qui a été vérifié, et ce qui ne l'a pas été »).

## Ce que vous allez obtenir

- **Un seul service** « proprefact » : il sert à la fois les écrans (le front-end) et l'API, à une seule adresse,
  par exemple `https://factures.votre-domaine.fr`. Il est construit à partir du fichier `Dockerfile` du dépôt.
- **Une base PostgreSQL** gérée par Railway, dans le même projet, joignable seulement par l'application.
- **Tout en Europe** (Amsterdam, Pays-Bas).
- **Mise à jour automatique** : chaque `git push` sur la branche `main` reconstruit et remet en ligne l'application.

## 0. Ce qu'il vous faut

| Quoi | Pourquoi |
|---|---|
| Le compte GitHub qui a accès au dépôt `Booster39/proprefact` | Railway récupère le code sur GitHub |
| Une carte bancaire | Railway est payant (voir ci-dessous) |
| Un compte **Brevo** (ou un autre service d'envoi d'e-mails par SMTP) | Envoyer les liens de confirmation d'inscription |
| Un nom de domaine (facultatif au début) | Une adresse à votre nom au lieu de `….up.railway.app` |
| Un terminal avec `openssl` (macOS, Linux ; sous Windows : Git Bash) | Fabriquer le secret `JWT_SECRET` |

### Quelle offre Railway choisir : **Pro**

⚠ **Décision à prendre par vous.** D'après la documentation de Railway (page « Outbound Networking »), **l'envoi
d'e-mails par SMTP n'est possible qu'avec l'offre Pro** (environ 20 $ par mois, dont 20 $ de consommation
incluse, d'après le forum de Railway en novembre 2026). Sur l'offre Hobby, les ports SMTP (25, 465, 587) sont
bloqués : les e-mails de confirmation ne partiraient jamais et **personne ne pourrait finir son inscription**.
Les **sauvegardes automatiques** de la base semblent elles aussi réservées à l'offre Pro.

- **Recommandé : l'offre Pro.** Rien à changer dans l'application.
- Autre possibilité : rester sur Hobby et envoyer les e-mails par l'API HTTPS d'un service (Brevo, Resend…). Cela
  demande de **modifier l'application** (elle ne sait aujourd'hui envoyer que par SMTP) : à demander à un
  développeur avant la mise en ligne.

## 1. Créer le compte et le projet

1. Allez sur <https://railway.com>, cliquez sur **Login**, puis connectez-vous **avec GitHub**.
2. Dans les réglages du compte (**Account / Workspace Settings → Plans** ou **Billing**), choisissez l'offre
   **Pro** et enregistrez la carte bancaire.
3. ⚠ Si les réglages de votre espace de travail proposent une **région par défaut**, choisissez
   **EU West (Amsterdam, Netherlands)**. Sinon, la région se règle service par service aux étapes 2 et 4.
4. Sur le tableau de bord, cliquez sur **New Project** puis **Empty Project**. Renommez le projet
   « proprefact » (**Settings** du projet → **Name**).

## 2. Ajouter la base PostgreSQL

1. Dans le projet, cliquez sur **Create** (ou **+ New**) → **Database** → **PostgreSQL**. Un service nommé
   **Postgres** apparaît. **Gardez ce nom** : les variables de l'étape 5 y font référence.
2. Ouvrez le service **Postgres** → **Settings** → **Regions** (ou **Deploy → Region**) : choisissez
   **EU West (Amsterdam, Netherlands)**. Les données de vos clients restent ainsi en Europe.
3. Ne touchez à rien d'autre. Railway crée lui-même l'utilisateur, le mot de passe et la base ; l'application les
   lira par des références (étape 5). **Ne recopiez jamais le mot de passe à la main.**

> Railway fournit aujourd'hui PostgreSQL 18. L'application a été essayée de bout en bout sur PostgreSQL 16 et 18.

## 3. Préparer l'envoi des e-mails (Brevo)

1. Créez un compte sur <https://www.brevo.com> (offre gratuite suffisante pour démarrer).
2. **Expéditeurs et domaines** : ajoutez et vérifiez l'adresse qui enverra les e-mails, par exemple
   `factures@votre-domaine.fr`. Idéalement, **authentifiez le domaine** (Brevo vous donne des enregistrements DNS
   SPF et DKIM à ajouter chez votre registraire) : sans cela, les e-mails risquent de finir en courrier indésirable.
3. **SMTP & API** → onglet **SMTP** : notez le serveur (`smtp-relay.brevo.com`), le port (`587`), l'**identifiant**
   SMTP (souvent de la forme `…@smtp-brevo.com`) et créez une **clé SMTP** (c'est le mot de passe). Gardez-la de
   côté, elle ne s'affiche qu'une fois.

## 4. Relier le dépôt GitHub

1. Dans le projet, **Create** (ou **+ New**) → **GitHub Repo**. Si Railway le demande, installez l'application
   GitHub de Railway et donnez-lui accès au dépôt **Booster39/proprefact** (seulement à celui-ci).
2. Choisissez **Booster39/proprefact**. Un service apparaît ; renommez-le **proprefact** (**Settings → Name**).
3. Railway commence tout de suite une construction : elle **échouera ou ne démarrera pas** tant que les variables
   ne sont pas saisies (étape 5). C'est normal.
4. Dans **Settings** du service **proprefact**, vérifiez ou réglez :

| Réglage | Valeur | Pourquoi |
|---|---|---|
| **Source → Branch** (branche déployée) | `main` | Seule `main` est mise en ligne |
| **Build → Builder** | **Dockerfile** (`Dockerfile` à la racine, détecté seul) | L'image contient le front et l'API |
| **Deploy → Region** | **EU West (Amsterdam, Netherlands)** | Données et application en Europe |
| **Deploy → Replicas** (nombre d'instances) | **1** | Voir l'encadré ci-dessous |
| **Deploy → Serverless** (ou « App Sleeping ») | **désactivé** | Voir l'encadré ci-dessous |
| **Deploy → Healthcheck Path** | `/actuator/health` | Railway attend « UP » avant de basculer sur la nouvelle version |
| **Deploy → Healthcheck Timeout** | `300` (secondes) | Laisse le temps des migrations au premier démarrage |
| **Deploy → Restart Policy** | **Always** | Relance automatique si l'application s'arrête |

> **Une seule instance, jamais en veille.** Ne montez jamais le nombre d'instances au-dessus de 1 et n'activez
> jamais « Serverless » :
> - la limite des tentatives de connexion (30 par adresse IP et par quart d'heure, 5 échecs par compte) est
>   gardée **en mémoire** dans l'application : avec deux instances, un attaquant aurait deux fois plus d'essais ;
> - la **relance des dépôts** vers la plateforme agréée tourne **chaque minute** dans l'application : en veille,
>   elle s'arrêterait.
>
> Pendant une mise à jour, Railway peut faire tourner l'ancienne et la nouvelle version quelques secondes en
> même temps ; c'est sans conséquence. En revanche, chaque mise à jour remet les compteurs de tentatives à zéro.

## 5. Saisir les variables

Service **proprefact** → onglet **Variables** → **Raw Editor**. Collez le bloc ci-dessous, **remplacez les
valeurs entre `<…>`**, puis **Update Variables** (ou **Save**).

```
PGHOST=${{Postgres.PGHOST}}
PGPORT=${{Postgres.PGPORT}}
PGDATABASE=${{Postgres.PGDATABASE}}
PGUSER=${{Postgres.PGUSER}}
PGPASSWORD=${{Postgres.PGPASSWORD}}
PORT=8080
ENTETE_IP_CLIENT=X-Real-IP
JWT_SECRET=<64 caractères aléatoires, voir ci-dessous>
APP_URL=<adresse publique, voir étape 6, sans / à la fin>
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USER=<identifiant SMTP Brevo>
MAIL_PASSWORD=<clé SMTP Brevo>
MAIL_EXPEDITEUR=<adresse vérifiée dans Brevo, par exemple factures@votre-domaine.fr>
```

| Variable | Explication |
|---|---|
| `PGHOST` … `PGPASSWORD` | **Références** : `${{Postgres.PGHOST}}` signifie « la valeur PGHOST du service Postgres ». Railway les remplit seul et les tient à jour. Si votre base porte un autre nom que `Postgres`, remplacez `Postgres` par ce nom. |
| `PORT` | Port sur lequel l'application écoute. Fixé à 8080 pour que le domaine (étape 6) pointe sans ambiguïté sur le bon port. |
| `ENTETE_IP_CLIENT` | **Exactement `X-Real-IP`.** Le proxy de Railway écrit dans cet en-tête l'adresse IP du visiteur ; l'application s'en sert pour limiter les tentatives de connexion. Ne mettez **jamais** `X-Forwarded-For` (un attaquant peut y inventer des adresses). Voir l'étape 8, vérification 6. |
| `JWT_SECRET` | Clé qui signe les connexions. **Aléatoire, 32 caractères minimum, jamais partagée, jamais commitée.** Dans un terminal : `openssl rand -hex 32` (64 caractères). La changer déconnecte tous les utilisateurs. |
| `APP_URL` | Adresse publique **avec `https://`**, sans `/` final : elle sert à fabriquer les liens des e-mails. D'abord l'adresse `….up.railway.app` de l'étape 6, puis votre domaine (étape 9). |
| `MAIL_*` | Les valeurs notées à l'étape 3. **Sans `MAIL_HOST`, aucun e-mail ne part** (ils sont seulement écrits dans le journal). |

Ne créez **pas** `SPRING_PROFILES_ACTIVE` : le profil `dev` contient un secret connu de tous.

Après l'enregistrement, Railway propose de redéployer (**Deploy** / **Apply changes**) : acceptez.

## 6. Donner une adresse publique

1. Service **proprefact** → **Settings** → **Networking** → **Public Networking** → **Generate Domain**.
   Si Railway demande un port, indiquez **8080**.
2. Railway affiche une adresse du type `https://proprefact-production-xxxx.up.railway.app`. Copiez-la.
3. Retournez dans **Variables**, mettez cette adresse dans `APP_URL` (sans `/` final), enregistrez et redéployez.

La base PostgreSQL, elle, ne doit **pas** avoir d'adresse publique : laissez sa partie **Networking** telle quelle.

## 7. Premier démarrage

Onglet **Deployments** du service **proprefact** : la construction prend quelques minutes (compilation du front,
puis du back-end). Cliquez sur le déploiement en cours puis sur **View logs** (journal) :

- dans le journal de construction (**Build Logs**), la dernière étape doit se terminer sans erreur ;
- dans le journal de l'application (**Deploy Logs**), vous devez voir `Successfully applied 5 migrations` au
  premier démarrage, puis `Started ProprefactApplication` ;
- le déploiement passe à l'état **Active** (ou **Success**) une fois la vérification de santé réussie.

Si le déploiement échoue, le journal dit pourquoi. Causes fréquentes : une variable manquante (le message nomme
la propriété, par exemple `JWT_SECRET` ou `PGHOST`), un `JWT_SECRET` trop court, une référence `${{Postgres.…}}`
avec un mauvais nom de service.

Si c'est la **construction** qui échoue (**Build Logs**) pendant un téléchargement (`npm ci`, ou Maven avec
`Could not transfer artifact` ou `Failed to fetch`), c'est en général un échec réseau passager entre Railway et
les dépôts de paquets : relancez avec **Redeploy** avant de chercher plus loin. Si l'erreur revient à
l'identique, demandez à un développeur.

## 8. Vérifier que tout marche

Avec l'adresse de l'étape 6 (remplacez `https://VOTRE-ADRESSE`) :

1. **Santé** : ouvrez `https://VOTRE-ADRESSE/actuator/health` → la page affiche `{"status":"UP"}`.
2. **Écrans** : ouvrez `https://VOTRE-ADRESSE` → l'écran de connexion de ProPreFact s'affiche. Ouvrez aussi
   `https://VOTRE-ADRESSE/factures` directement → l'application s'affiche (elle vous renvoie vers la connexion).
3. **API protégée** : ouvrez `https://VOTRE-ADRESSE/api/clients` → erreur 401 (normal : pas connecté).
4. **Inscription** : inscrivez-vous avec une vraie adresse e-mail. L'e-mail de confirmation doit arriver
   (regardez aussi les indésirables). Le lien doit commencer par votre `APP_URL`. Cliquez-le : vous êtes connecté.
   S'il n'arrive pas : dans le journal de l'application, `E-mail NON ENVOYÉ` signifie que `MAIL_HOST` est
   vide ; une erreur `Mail…Exception` (souvent `MailConnectException` ou une authentification refusée) signifie
   que le serveur SMTP est injoignable (offre Hobby, voir étape 0) ou que l'identifiant ou la clé Brevo est faux.
5. **Facture** : créez un client, un site, un contrat, générez les factures du mois, validez-en une,
   téléchargez le Factur-X (un PDF). Supprimez ensuite ces données d'essai si vous le souhaitez.
6. **Limite de tentatives** ⚠ (point de sécurité, une fois pour toutes). La documentation de Railway dit que
   son proxy **écrit** l'adresse du visiteur dans `X-Real-IP`, mais seul le forum de Railway affirme qu'il
   **remplace** une valeur envoyée par un attaquant. Vérifiez-le depuis un terminal (cela bloque votre propre
   adresse pendant 15 minutes sur la connexion et l'inscription ; un redéploiement remet les compteurs à zéro) :

   ```bash
   for i in $(seq 1 35); do
     curl -s -o /dev/null -w "%{http_code} " -X POST https://VOTRE-ADRESSE/api/auth/renvoi-confirmation \
       -H 'Content-Type: application/json' -H "X-Real-IP: 203.0.113.$i" -H "X-Forwarded-For: 198.51.100.$i" \
       -d '{"email":"essai@exemple.fr"}'
   done; echo
   ```

   - **Résultat attendu** : `202` trente fois, puis `429` (trop de tentatives) : l'adresse inventée est ignorée.
     C'est bon.
   - Si vous obtenez **35 fois `202`** : le proxy laisse passer l'adresse inventée, la limite par IP est
     contournable. Mettez alors `ENTETE_IP_CLIENT` **vide** et prévenez un développeur : la limite par compte
     (5 échecs par e-mail) protège encore les mots de passe, mais la limite par IP devient commune à tous les
     visiteurs.

## 9. Nom de domaine personnalisé

1. Service **proprefact** → **Settings** → **Networking** → **Custom Domain** : saisissez par exemple
   `factures.votre-domaine.fr` (un sous-domaine est le plus simple), port **8080** si demandé.
2. Railway affiche un ou plusieurs enregistrements DNS à créer (en général un **CNAME** vers une adresse
   `….up.railway.app`, parfois un **TXT** de vérification). Créez-les **exactement** chez votre registraire
   (OVH, Gandi, …), dans la zone DNS du domaine.
3. Attendez que Railway affiche le domaine comme vérifié (de quelques minutes à quelques heures). Le certificat
   HTTPS est créé automatiquement par Railway.
4. Mettez la nouvelle adresse dans `APP_URL` (avec `https://`, sans `/` final), enregistrez et redéployez.
   Refaites les vérifications 1 à 4 de l'étape 8 avec la nouvelle adresse.
5. Si vous passez par Cloudflare, choisissez « DNS only » (nuage gris) au début : ⚠ un proxy supplémentaire
   devant Railway peut changer l'adresse IP vue par l'application ; refaites alors la vérification 6.

## 10. Sauvegardes de la base

1. Service **Postgres** → onglet **Backups** : programmez une sauvegarde **quotidienne** et une
   **hebdomadaire** (offre Pro). D'après la documentation, Railway garde les quotidiennes environ 6 jours, les
   hebdomadaires environ un mois et les mensuelles environ 3 mois.
2. Essayez **une restauration** une fois, au début, pour savoir faire le jour où il le faudra (la documentation
   de Railway, page « Back Up and Restore Postgres », décrit la procédure).
3. ⚠ Ces sauvegardes restent chez Railway. Pour une copie chez vous (recommandé pour des factures, à conserver
   10 ans), un développeur peut programmer un export régulier `pg_dump` vers un stockage externe (Railway en
   propose un modèle, « Postgres S3 Backups »). Les fichiers Factur-X émis sont dans la base : sauvegarder la
   base suffit.

## 11. Mettre à jour l'application

- **Chaque `git push` sur `main`** déclenche une nouvelle construction et un nouveau déploiement. Railway attend
  que `/actuator/health` réponde « UP » avant de basculer : si la nouvelle version ne démarre pas, l'ancienne
  reste en ligne.
- Les changements de la base (migrations Flyway) s'appliquent tout seuls au démarrage.
- **Avant de pousser sur `main`**, vérifiez que l'intégration continue GitHub est verte (onglet **Actions** du
  dépôt) : elle construit la même image et la lance de bout en bout contre PostgreSQL.
- **Revenir en arrière** : onglet **Deployments**, sur un déploiement précédent réussi → **Redeploy** (ou
  **Rollback**). ⚠ Si la version récente a modifié la base, un retour arrière peut ne pas suffire : demandez à
  un développeur.
- La branche `railway` (où ce travail a été préparé) n'est **pas** déployée : seule `main` l'est.

## 12. (Facultatif) La configuration en fichier : `.railway/railway.ts`

Le dépôt contient `.railway/railway.ts`, qui décrit les réglages des étapes 2, 4 et 5 (sauf les secrets) : base
Postgres, service « proprefact » construit depuis le `Dockerfile`, santé, redémarrage, une instance en Europe,
pas de veille, variables `PG*` et `ENTETE_IP_CLIENT`.

- ⚠ Railway a **remplacé** `railway.json` / `railway.toml` par ce format : d'après sa documentation, les anciens
  fichiers ne sont plus lus à partir du 1er décembre 2026 et un nouveau service ne peut plus les adopter. Le
  nouveau format est encore en **bêta**.
- Il ne s'applique **pas** à chaque `git push` : on l'applique à la main, depuis un ordinateur, avec la CLI
  Railway (version 5.42.1 ou plus) :

  ```bash
  npm install -g @railway/cli
  railway login
  railway link            # choisir le projet « proprefact » et l'environnement « production »
  railway config plan     # AFFICHE les différences, ne change rien
  railway config apply    # applique (demande confirmation)
  ```

- **Lisez le résultat de `railway config plan` avant d'appliquer.** Si les étapes 2 à 5 ont été suivies, il doit
  annoncer peu ou pas de changements. S'il veut **supprimer** ou **recréer** la base ou le service (par exemple
  parce qu'ils ne portent pas les noms `Postgres` et `proprefact`), **n'appliquez pas** : renommez-les ou
  demandez à un développeur. Si la région `europe-west4-drams3a` est refusée, prenez l'identifiant exact affiché
  dans les réglages du service et corrigez-le dans le fichier.
- Les secrets (`JWT_SECRET`, `APP_URL`, `MAIL_*`) restent saisis dans le tableau de bord ; le fichier les garde
  tels quels (`preserve()`).
- Vous pouvez aussi **ne jamais l'utiliser** : les réglages faits à la main dans le tableau de bord suffisent.
  Dans ce cas, le fichier sert de mémo des bons réglages.

## Règles à ne jamais enfreindre

- **Une seule instance**, **Serverless désactivé** (voir l'encadré de l'étape 4).
- `ENTETE_IP_CLIENT=X-Real-IP` et rien d'autre (jamais `X-Forwarded-For`).
- Aucun secret dans le dépôt Git : ils ne vivent que dans les variables Railway.
- Pas de profil `dev` en production.
- Pas d'adresse publique pour la base de données.

## Ce qui a été vérifié, et ce qui ne l'a pas été

Vérifié (le 19 novembre 2026, sans Railway) :

- L'image du `Dockerfile` est construite **en entier** par l'intégration continue GitHub, puis lancée contre
  PostgreSQL 16 et 18 avec les mêmes variables `PG*` que Railway et le port 8080 : santé « UP », accueil,
  `/factures`, `/api/clients` en 401, inscription, lien de confirmation, client, contrat, facture validée,
  Factur-X téléchargé (`deploiement/essai.sh`).
- Le port est bien lu dans `PORT` (essai avec `PORT=9090`) ; l'application tourne sans droits administrateur.
- La limite de tentatives ne peut pas être contournée par un en-tête `X-Forwarded-For` inventé, et un en-tête
  `X-Real-IP` malformé est ignoré (tests automatiques `AdresseIpTest`).
- `.railway/railway.ts` est conforme aux types du SDK officiel de Railway (`railway` 3.17.0) et produit les
  réglages attendus.

**Pas** vérifié (aucun compte Railway) :

- tout ce qui se passe **chez Railway** : création du projet, noms exacts des menus, références de variables,
  vérification de santé, région, domaine, certificat, sauvegardes, `railway config plan/apply` ;
- que le proxy de Railway **remplace** un `X-Real-IP` envoyé par un attaquant (vérification 6 de l'étape 8) ;
- l'envoi réel d'un e-mail par Brevo depuis Railway (et le blocage SMTP de l'offre Hobby, connu seulement par la
  documentation) ;
- l'identifiant exact de la région Europe (`europe-west4-drams3a`), signalé comme refusé par quelques
  utilisateurs sur le forum de Railway.

Sources consultées (documentation et forum de Railway) : « Public Networking — Specs & Limits »,
« Outbound Networking », « Config as Code », « Infrastructure as Code », « Back Up and Restore Postgres »,
« Deployment Regions », « Serverless », et le README du SDK `railway` (paquet npm officiel).
