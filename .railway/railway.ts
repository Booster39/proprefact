// Configuration de ProPreFact sur Railway (« Infrastructure as Code », format qui remplace railway.json).
// Elle ne s'applique PAS toute seule à chaque git push : on la compare puis on l'applique avec la CLI Railway
// (`railway config plan`, puis `railway config apply`), voir DEPLOIEMENT-RAILWAY.md, étape 9.
// Écrite et vérifiée (tsc) contre le SDK `railway` 3.17.0, dont l'API IaC est encore en bêta.
//
// UNE SEULE INSTANCE, JAMAIS EN VEILLE. Ne pas augmenter le nombre d'instances ni activer « Serverless » :
// - la limite de tentatives de connexion est en mémoire, propre à chaque instance : avec deux instances, un
//   attaquant aurait deux fois plus d'essais, et un redémarrage remet les compteurs à zéro ;
// - la relance des dépôts vers la plateforme agréée tourne chaque minute dans l'application : en veille, elle
//   ne tournerait plus.
import { defineRailway, github, postgres, preserve, project, service } from 'railway/iac';

// Amsterdam (EU West). Identifiant à vérifier dans les réglages du service si `railway config plan` le refuse.
const EUROPE = 'europe-west4-drams3a';

export default defineRailway(() => {
  const base = postgres('Postgres', { region: EUROPE });

  const application = service('proprefact', {
    source: github('Booster39/proprefact', { branch: 'main' }),
    build: { builder: 'DOCKERFILE', dockerfilePath: 'Dockerfile' },
    // Sans connexion, sans détail ; répond une fois l'application démarrée et la base jointe.
    healthcheck: '/actuator/health',
    healthcheckTimeout: 300,
    regions: { [EUROPE]: 1 },
    deploy: {
      sleepApplication: false,
      restartPolicyType: 'ALWAYS',
    },
    env: {
      // Base : références vers le service PostgreSQL (jamais de valeur copiée à la main).
      PGHOST: base.env.PGHOST,
      PGPORT: base.env.PGPORT,
      PGDATABASE: base.env.PGDATABASE,
      PGUSER: base.env.PGUSER,
      PGPASSWORD: base.env.PGPASSWORD,
      // Le proxy de Railway écrit l'adresse IP du client dans X-Real-IP.
      ENTETE_IP_CLIENT: 'X-Real-IP',
      // Secrets et adresses propres au propriétaire : saisis dans le tableau de bord, jamais dans ce fichier.
      // preserve() garde la valeur déjà enregistrée sur Railway.
      JWT_SECRET: preserve(),
      APP_URL: preserve(),
      MAIL_HOST: preserve(),
      MAIL_PORT: preserve(),
      MAIL_USER: preserve(),
      MAIL_PASSWORD: preserve(),
      MAIL_EXPEDITEUR: preserve(),
    },
  });

  return project('proprefact', { resources: [base, application] });
});
