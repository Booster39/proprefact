#!/usr/bin/env bash
# Construit et vérifie le back-end et le front-end, puis les images Docker.
# À lancer depuis n'importe quel dossier ; Docker doit tourner (les tests du back-end en ont besoin).
set -euo pipefail
racine="$(cd "$(dirname "$0")/.." && pwd)"

echo "== Back-end : tests et jar"
(cd "$racine/backend" && ./mvnw -B verify)

echo "== Front-end : vérifications et compilation"
(cd "$racine/frontend" && npm ci && npm run format:check && npm run lint && npm test && npx ng build)

echo "== Images Docker"
# Construites sans le fichier de lancement : la construction n'a besoin d'aucun secret.
docker build -t proprefact-backend "$racine/backend"
docker build -t proprefact-frontend "$racine/frontend"

echo "Terminé. Lancement : docker compose -f deploiement/docker-compose.yml up -d"
