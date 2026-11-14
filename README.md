# ProPreFact

Facturation récurrente pour les entreprises de propreté, avec factures au format Factur-X
(facturation électronique obligatoire en France).

## Démarrer en développement

Prérequis : Java 21, Node 22, Docker.

```bash
docker compose up -d
```

```bash
cd backend && SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npm start
```

L'application est sur http://localhost:4200 (les appels `/api` sont relayés vers le port 8080).
À l'inscription, le lien de confirmation s'affiche dans le journal du back-end.

## Tests

```bash
cd backend && ./mvnw verify
cd frontend && npm run format:check && npm run lint && npm test
```

Le test `ParcoursFacturationTest` déroule le parcours complet (inscription, clients, contrats,
facturation mensuelle, validation, Factur-X) et vérifie le cloisonnement entre entreprises.

## Organisation

- `backend/` : Spring Boot 4, PostgreSQL, Flyway. Une seule base pour toutes les entreprises ;
  chaque ligne porte un `entreprise_id` que Hibernate filtre automatiquement (`TenantResolver`, `@TenantId`).
- `frontend/` : Angular 22.
- `backend/src/main/java/com/proprefact/service/FacturXService.java` : PDF/A-3 + XML EN 16931 (bibliothèque Mustang).
- Une facture validée est figée : elle garde sa copie des coordonnées vendeur et acheteur, et le fichier
  Factur-X émis est conservé en base (`facture_document`) puis resservi à l'identique.
- `backend/src/main/java/com/proprefact/service/DepotService.java` : dépôt en trois temps (intention enregistrée,
  appel hors transaction, résultat enregistré), rejoué chaque minute avec la même clé d'idempotence tant qu'il échoue.
- `backend/src/main/java/com/proprefact/service/PlateformeAgreee.java` : point d'intégration de la
  plateforme agréée. L'implémentation actuelle est **simulée** et ne transmet rien.

## Configuration de production

| Variable | Rôle |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL |
| `JWT_SECRET` | Secret de signature des jetons, 32 caractères minimum, obligatoire |
| `APP_URL` | Adresse publique du front-end, utilisée dans les liens envoyés par e-mail |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_EXPEDITEUR` | Serveur SMTP d'envoi des e-mails |
| `FORWARD_HEADERS` | `framework` derrière un proxy inverse, pour limiter les tentatives sur la vraie adresse IP |

Sans `MAIL_HOST`, les e-mails (dont les liens de confirmation) sont seulement écrits dans le journal :
pratique en développement, à ne jamais laisser en production.

Ne pas activer le profil `dev` en production : il contient un secret connu.

## Reste à faire avant de vendre

- Brancher une vraie plateforme agréée (dépôt, statuts, réception des factures fournisseurs).
- Faire valider les mentions et le XML par cette plateforme (catégorie de l'opération, adresse électronique).
- Avoirs, modification des brouillons, prorata des mois incomplets, factures de plus d'une page.
- Devis, relances de paiement, export comptable, abonnements Stripe.
- Réinitialisation du mot de passe, plusieurs utilisateurs par entreprise.
- Limites de tentatives partagées entre instances (elles sont aujourd'hui en mémoire, par instance).
