# Mettre ProPreFact en ligne sur Clever Cloud

Ce guide s'adresse à quelqu'un qui n'a jamais utilisé Clever Cloud. Suivez les étapes dans l'ordre ; chacune dit
quoi vérifier avant de passer à la suivante.

**À savoir avant de commencer.** Ce guide a été écrit le 19 novembre 2026, **sans compte Clever Cloud** : rien
n'a été déployé pour de vrai, et le site de Clever Cloud n'était pas lisible depuis l'environnement de travail.
Les faits viennent des extraits de sa documentation publique trouvés par recherche (sources à la fin). Les noms
de menus peuvent donc différer de ce que vous verrez ; ce qui compte, ce sont les valeurs à saisir. Les points
incertains sont signalés par « ⚠ ». Ce qui a réellement été vérifié est listé à la fin.

## Ce que vous allez obtenir

- **Une application Docker** « proprefact » : elle sert à la fois les écrans (le front-end) et l'API, à une seule
  adresse, par exemple `https://factures.votre-domaine.fr`. Elle est construite à partir du fichier `Dockerfile`
  du dépôt.
- **Une base PostgreSQL** gérée par Clever Cloud (module complémentaire, « add-on »), liée à l'application.
- **Tout à Paris**, chez un hébergeur français.
- **Mise à jour automatique** : chaque `git push` sur la branche `main` reconstruit et remet en ligne l'application.

## 0. Ce qu'il vous faut

| Quoi | Pourquoi |
|---|---|
| Le compte GitHub qui a accès au dépôt `Booster39/proprefact` | Clever Cloud récupère le code sur GitHub |
| Une carte bancaire | L'application et une base de production sont payantes |
| Un compte **Brevo** (ou un autre service d'envoi d'e-mails par SMTP) | Envoyer les liens de confirmation d'inscription |
| Un nom de domaine (facultatif au début) | Une adresse à votre nom au lieu de `….cleverapps.io` |
| Un terminal avec `openssl` (macOS, Linux ; sous Windows : Git Bash) | Fabriquer le secret `JWT_SECRET` |

⚠ **Envoi d'e-mails.** Aucune page trouvée de Clever Cloud ne dit si l'envoi par SMTP (port 587) est permis ou
bloqué depuis les applications. La vérification 4 de l'étape 8 le montre en quelques minutes. S'il est bloqué :
Brevo accepte aussi le port `2525` (mettez `MAIL_PORT=2525`) ; sinon, il faudra faire écrire par un développeur
un envoi par l'API HTTPS de Brevo (l'application ne sait aujourd'hui envoyer que par SMTP).

## 1. Créer le compte

1. Allez sur <https://www.clever-cloud.com>, créez un compte **avec GitHub** (c'est ce qui permettra à Clever
   Cloud de voir le dépôt).
2. Dans votre espace (**Personal space**, ou une organisation créée pour l'entreprise), ouvrez la facturation
   (**Billing**) et enregistrez la carte bancaire.

## 2. Créer l'application

1. **Create** → **an application** → **Create an application from a GitHub repository** (ou « Select a GitHub
   repository ») → choisissez **Booster39/proprefact**.
2. Type d'application : **Docker**.
3. Taille des instances : **une instance**, taille **XS** ou plus (⚠ au moins 1 Go de mémoire : Java et Spring
   Boot ne tiennent pas dans les tailles « pico » et « nano »). Désactivez l'ajustement automatique (même
   nombre minimum et maximum d'instances : **1** ; même taille minimum et maximum).
4. Zone : **Paris** (`par`).
5. Nom : **proprefact**.
6. Clever Cloud propose ensuite d'ajouter un module : passez à l'étape 3 (vous pouvez aussi le faire depuis
   ce menu).
7. ⚠ Si une première construction démarre tout de suite, elle **échouera** tant que les variables ne sont pas
   saisies (étape 4). C'est normal.

> **Une seule instance, jamais plus.** Ne montez jamais le nombre d'instances au-dessus de 1 :
> - la limite des tentatives de connexion (30 par adresse IP et par quart d'heure, 5 échecs par compte) est
>   gardée **en mémoire** dans l'application : avec deux instances, un attaquant aurait deux fois plus d'essais ;
> - la **relance des dépôts** vers la plateforme agréée tourne **chaque minute** dans l'application.
>
> Pendant une mise à jour, Clever Cloud démarre la nouvelle version avant d'arrêter l'ancienne : les deux
> tournent quelques secondes ensemble, c'est sans conséquence. En revanche, chaque mise à jour remet les compteurs
> de tentatives à zéro.

## 3. Ajouter la base PostgreSQL

1. **Create** → **an add-on** → **PostgreSQL**.
2. Offre : une offre **dédiée** (la plus petite suffit au début). **Pas l'offre « DEV »** : d'après la
   documentation, elle est gratuite mais réservée aux essais.
3. Version : **18** (l'application a été essayée de bout en bout sur PostgreSQL 16 et 18).
4. Zone : **Paris**.
5. **Liez-la à l'application proprefact** (Clever Cloud le propose à la création ; sinon, dans l'application :
   **Service dependencies** → **Link add-ons**).
6. Vérifiez : application **proprefact** → **Environment variables** : les variables `POSTGRESQL_ADDON_HOST`,
   `POSTGRESQL_ADDON_PORT`, `POSTGRESQL_ADDON_DB`, `POSTGRESQL_ADDON_USER` et `POSTGRESQL_ADDON_PASSWORD`
   apparaissent (dans la partie des modules liés). L'application les lit d'elle-même : **ne les recopiez pas**,
   et ne recopiez jamais le mot de passe à la main.

## 4. Saisir les variables

D'abord, préparez les e-mails (Brevo) :

1. Créez un compte sur <https://www.brevo.com> (offre gratuite suffisante pour démarrer).
2. **Expéditeurs et domaines** : ajoutez et vérifiez l'adresse qui enverra les e-mails, par exemple
   `factures@votre-domaine.fr`. Idéalement, **authentifiez le domaine** (Brevo vous donne des enregistrements DNS
   SPF et DKIM à ajouter chez votre registraire) : sans cela, les e-mails risquent de finir en courrier indésirable.
3. **SMTP & API** → onglet **SMTP** : notez le serveur (`smtp-relay.brevo.com`), le port (`587`), l'**identifiant**
   SMTP (souvent de la forme `…@smtp-brevo.com`) et créez une **clé SMTP** (c'est le mot de passe). Gardez-la de
   côté, elle ne s'affiche qu'une fois.

Puis application **proprefact** → **Environment variables** → mode **Expert** (zone de texte). Ajoutez ce bloc
sous les lignes existantes, **remplacez les valeurs entre `<…>`**, puis **Update changes** (ou **Save**) :

```
CC_HEALTH_CHECK_PATH=/actuator/health
ENTETE_IP_CLIENT=X-Forwarded-For
JWT_SECRET=<64 caractères aléatoires, voir ci-dessous>
APP_URL=<adresse publique, voir étape 5, sans / à la fin>
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USER=<identifiant SMTP Brevo>
MAIL_PASSWORD=<clé SMTP Brevo>
MAIL_EXPEDITEUR=<adresse vérifiée dans Brevo, par exemple factures@votre-domaine.fr>
```

| Variable | Explication |
|---|---|
| `CC_HEALTH_CHECK_PATH` | Variable de Clever Cloud : à chaque déploiement, il appelle cette adresse et n'ouvre la nouvelle version au public que si elle répond « 200 ». Si la nouvelle version ne démarre pas, l'ancienne reste en ligne. |
| `ENTETE_IP_CLIENT` | **Exactement `X-Forwarded-For`.** Le proxy de Clever Cloud ajoute l'adresse IP du visiteur **à la fin** de cet en-tête ; l'application ne lit que cette dernière adresse, pour limiter les tentatives de connexion. Voir l'étape 8, vérification 6. |
| `JWT_SECRET` | Clé qui signe les connexions. **Aléatoire, 32 caractères minimum, jamais partagée, jamais commitée.** Dans un terminal : `openssl rand -hex 32` (64 caractères). La changer déconnecte tous les utilisateurs. |
| `APP_URL` | Adresse publique **avec `https://`**, sans `/` final : elle sert à fabriquer les liens des e-mails. D'abord l'adresse `….cleverapps.io` de l'étape 5, puis votre domaine (étape 9). |
| `MAIL_*` | Les valeurs notées plus haut. **Sans `MAIL_HOST`, aucun e-mail ne part** (ils sont seulement écrits dans le journal). |

- Ne créez **pas** `SPRING_PROFILES_ACTIVE` : le profil `dev` contient un secret connu de tous.
- Ne créez **pas** `PORT` ni `CC_DOCKER_EXPOSED_HTTP_PORT` : l'application écoute sur 8080, le port qu'attend
  Clever Cloud.

## 5. Adresse publique et branche déployée

1. Application **proprefact** → **Domain names** : Clever Cloud a déjà donné une adresse du type
   `app-xxxxxxxx.cleverapps.io`. Copiez-la (avec `https://`) dans `APP_URL` (étape 4).
2. ⚠ Application **proprefact** → **Information** : activez l'option **Force HTTPS** (les visiteurs en `http://`
   sont renvoyés vers `https://`).
3. Toujours dans **Information**, réglez la branche GitHub déployée (« Used GitHub branch for deployment ») sur
   **`main`** : seule `main` est mise en ligne.

La base PostgreSQL n'a rien à régler ici.

## 6. (Facultatif) Construction plus rapide

La construction compile le front puis le back-end. Si elle échoue faute de mémoire (« Killed », « out of
memory », « JavaScript heap out of memory » dans le journal), ou si elle est très lente, choisissez une instance de
construction plus grande : **Scalability** → taille de l'instance de construction (**build instance**). ⚠ Le nom
exact du réglage n'a pas été vérifié.

## 7. Premier démarrage

Application **proprefact** → **Overview** → **Restart** (ou **Re-build and restart**). Ouvrez **Logs** :

- la construction (`docker build`) doit aller jusqu'au bout sans erreur ;
- ensuite, vous devez voir `Successfully applied 5 migrations` au premier démarrage, puis
  `Started ProprefactApplication` ;
- le déploiement est réussi quand Clever Cloud l'indique (vérification de santé passée).

Si le déploiement échoue, le journal dit pourquoi. Causes fréquentes : une variable manquante (le message nomme
la propriété, par exemple `JWT_SECRET` ou `POSTGRESQL_ADDON_HOST` — dans ce cas la base n'est pas liée, voir
l'étape 3), un `JWT_SECRET` trop court.

Si c'est la **construction** qui échoue pendant un téléchargement (`npm ci`, ou Maven avec
`Could not transfer artifact` ou `Failed to fetch`), c'est en général un échec réseau passager entre Clever Cloud
et les dépôts de paquets : relancez (**Re-build and restart**) avant de chercher plus loin. Si l'erreur revient
à l'identique, demandez à un développeur.

## 8. Vérifier que tout marche

Avec l'adresse de l'étape 5 (remplacez `https://VOTRE-ADRESSE`) :

1. **Santé** : ouvrez `https://VOTRE-ADRESSE/actuator/health` → la page affiche `{"status":"UP"}`.
2. **Écrans** : ouvrez `https://VOTRE-ADRESSE` → l'écran de connexion de ProPreFact s'affiche. Ouvrez aussi
   `https://VOTRE-ADRESSE/factures` directement → l'application s'affiche (elle vous renvoie vers la connexion).
3. **API protégée** : ouvrez `https://VOTRE-ADRESSE/api/clients` → erreur 401 (normal : pas connecté).
4. **Inscription** : inscrivez-vous avec une vraie adresse e-mail. L'e-mail de confirmation doit arriver
   (regardez aussi les indésirables). Le lien doit commencer par votre `APP_URL`. Cliquez-le : vous êtes connecté.
   S'il n'arrive pas : dans le journal de l'application, `E-mail NON ENVOYÉ` signifie que `MAIL_HOST` est
   vide ; une erreur `Mail…Exception` (souvent `MailConnectException` ou une authentification refusée) signifie
   que le serveur SMTP est injoignable (port bloqué, voir l'étape 0) ou que l'identifiant ou la clé Brevo est faux.
5. **Facture** : créez un client, un site, un contrat, générez les factures du mois, validez-en une,
   téléchargez le Factur-X (un PDF). Supprimez ensuite ces données d'essai si vous le souhaitez.
6. **Limite de tentatives** ⚠ (point de sécurité, une fois pour toutes). La documentation de Clever Cloud dit
   que son proxy ajoute l'adresse du visiteur à `X-Forwarded-For` ; l'application ne lit que la **dernière**
   adresse de cet en-tête. Vérifiez-le depuis un terminal (cela bloque votre propre adresse pendant 15 minutes sur
   la connexion et l'inscription ; un redéploiement remet les compteurs à zéro) :

   ```bash
   for i in $(seq 1 35); do
     curl -s -o /dev/null -w "%{http_code} " -X POST https://VOTRE-ADRESSE/api/auth/renvoi-confirmation \
       -H 'Content-Type: application/json' -H "X-Forwarded-For: 198.51.100.$i" -H "X-Real-IP: 203.0.113.$i" \
       -d '{"email":"essai@exemple.fr"}'
   done; echo
   ```

   - **Résultat attendu** : `202` trente fois, puis `429` (trop de tentatives) : les adresses inventées sont
     ignorées. Puis, **pendant ces 15 minutes**, depuis un **autre** réseau (un téléphone en 4G, sans Wi-Fi),
     ouvrez l'application et demandez un nouvel e-mail de confirmation (ou inscrivez-vous) : cela doit
     **marcher**. Les deux résultats ensemble montrent que chaque visiteur a bien son propre compteur. C'est bon.
   - Si vous obtenez **35 fois `202`** : l'adresse inventée est prise en compte, la limite par IP est
     contournable. Mettez alors `ENTETE_IP_CLIENT` **vide** et prévenez un développeur.
   - Si le téléphone est **aussi** refusé (429) : l'application voit la même adresse pour tout le monde (celle
     d'un proxy interne). Mettez `ENTETE_IP_CLIENT` **vide** et prévenez un développeur.
   - Avec `ENTETE_IP_CLIENT` vide, la limite par compte (5 échecs par e-mail) protège encore les mots de passe,
     mais la limite par IP devient commune à tous les visiteurs.

## 9. Nom de domaine personnalisé

1. Application **proprefact** → **Domain names** → ajoutez par exemple `factures.votre-domaine.fr` (un
   sous-domaine est le plus simple).
2. Chez votre registraire (OVH, Gandi, …), dans la zone DNS du domaine, créez un enregistrement **CNAME** de
   `factures` vers **`domain.par.clever-cloud.com.`** (adresse donnée par la documentation pour la zone Paris ;
   Clever Cloud l'affiche aussi sur la page **Domain names**). Pour le domaine nu (`votre-domaine.fr` sans
   sous-domaine), un CNAME n'est pas possible : Clever Cloud indique alors des enregistrements **A**.
3. Clever Cloud crée le certificat HTTPS automatiquement (Let's Encrypt) une fois le DNS en place (de quelques
   minutes à quelques heures).
4. Mettez la nouvelle adresse dans `APP_URL` (avec `https://`, sans `/` final), enregistrez et redémarrez.
   Refaites les vérifications 1 à 4 de l'étape 8 avec la nouvelle adresse.
5. Si vous passez par Cloudflare, choisissez « DNS only » (nuage gris) : ⚠ un proxy supplémentaire devant Clever
   Cloud changerait l'adresse IP vue par l'application ; refaites alors la vérification 6.

## 10. Sauvegardes de la base

1. D'après la documentation, Clever Cloud fait **une sauvegarde par jour, gardée 7 jours**, sans rien à régler.
   Elles sont visibles dans le module PostgreSQL → **Backups**, d'où on peut les télécharger. Une durée de
   conservation plus longue se demande au support de Clever Cloud.
2. Essayez **une restauration** une fois, au début (sur une base d'essai), pour savoir faire le jour où il le
   faudra.
3. ⚠ Ces sauvegardes restent chez Clever Cloud et ne durent que 7 jours. Pour des factures, à conserver
   **10 ans**, téléchargez régulièrement une sauvegarde et rangez-la ailleurs, ou faites programmer par un
   développeur un export `pg_dump` vers un stockage externe. Les fichiers Factur-X émis sont dans la base :
   sauvegarder la base suffit.

## 11. Mettre à jour l'application

- **Chaque `git push` sur `main`** déclenche une nouvelle construction et un nouveau déploiement. Clever Cloud
  attend que `/actuator/health` réponde avant de basculer : si la nouvelle version ne démarre pas, l'ancienne
  reste en ligne.
- Les changements de la base (migrations Flyway) s'appliquent tout seuls au démarrage.
- **Avant de pousser sur `main`**, vérifiez que l'intégration continue GitHub est verte (onglet **Actions** du
  dépôt) : elle construit la même image et la lance de bout en bout contre PostgreSQL.
- **Revenir en arrière** : ⚠ dans **Overview** ou **Activity**, Clever Cloud liste les déploiements et permet d'en
  relancer un précédent ; sinon, annulez le commit sur `main` (`git revert`) et poussez. ⚠ Si la version récente
  a modifié la base, un retour arrière peut ne pas suffire : demandez à un développeur.

## Règles à ne jamais enfreindre

- **Une seule instance** (voir l'encadré de l'étape 2).
- `ENTETE_IP_CLIENT=X-Forwarded-For` (vide seulement si la vérification 6 de l'étape 8 échoue).
- Aucun secret dans le dépôt Git : ils ne vivent que dans les variables Clever Cloud.
- Pas de profil `dev` en production.
- Pas d'offre « DEV » pour la base de production.

## Ce qui a été vérifié, et ce qui ne l'a pas été

Vérifié (le 19 novembre 2026, sans Clever Cloud) :

- L'image du `Dockerfile` est construite **en entier** par l'intégration continue GitHub, puis lancée contre
  PostgreSQL 16 et 18 avec les mêmes variables `POSTGRESQL_ADDON_*` que Clever Cloud et le port 8080 : santé
  « UP », accueil, `/factures`, `/api/clients` en 401, inscription, lien de confirmation, client, contrat, facture
  validée, Factur-X téléchargé (`deploiement/essai.sh`).
- La lecture des variables `POSTGRESQL_ADDON_*` (test `ConnexionBaseTest`).
- La limite de tentatives ne peut pas être contournée par une adresse inventée dans `X-Forwarded-For`, à
  condition que le proxy ajoute la vraie adresse à la fin ; une valeur malformée est ignorée (tests automatiques
  `AdresseClientTest` et `AdresseIpTest`).

**Pas** vérifié (aucun compte Clever Cloud, site de Clever Cloud illisible depuis l'environnement de travail) :

- tout ce qui se passe **chez Clever Cloud** : noms exacts des menus, construction Docker sur leurs machines
  (mémoire, durée), vérification de santé, domaine, certificat, sauvegardes, retour arrière ;
- que le proxy de Clever Cloud **ajoute** bien l'adresse réelle **à la fin** de `X-Forwarded-For`, et qu'aucun
  autre proxy interne ne s'intercale (vérification 6 de l'étape 8) ;
- que l'envoi par SMTP (port 587) est permis depuis Clever Cloud, et l'envoi réel d'un e-mail par Brevo ;
- que les applications payantes ne sont jamais mises en veille, et les prix.

Sources (documentation de Clever Cloud, vue par extraits de recherche) : pages « Docker », « Deployment
healthcheck path », « PostgreSQL », « Environment Variable Reference », « Domain Names », « Deploy from GitHub »,
« FAQ » (en-tête `X-Forwarded-For`), « Clever Tools — CLI commands reference » (tailles et nombre d'instances),
article « Force HTTPS on Clever Cloud applications » ; dépôt GitHub du proxy Sōzu (pull request n° 1685 : la
partie de `X-Forwarded-For` reçue du client n'est pas fiable).
