#!/usr/bin/env bash
# Essai de bout en bout de l'image de production contre PostgreSQL, à travers une seule adresse : accueil, pages
# de l'application, API protégée, inscription, confirmation par le lien du journal, client, contrat, facture
# validée, Factur-X téléchargé. Utilisé par l'intégration continue ; à lancer aussi avant une mise en production.
#
# Pile jetable (projet Docker « proprefact-essai », secrets aléatoires, port 18080), supprimée à la fin avec ses
# données : la pile de production locale n'est pas touchée.
#   ./deploiement/essai.sh                      construit l'image puis l'essaie
#   SANS_CONSTRUCTION=1 ./deploiement/essai.sh  essaie l'image « proprefact » déjà construite
#   POSTGRES_VERSION=18 ./deploiement/essai.sh  essaie avec PostgreSQL 18 (version conseillée chez Clever Cloud)
set -euo pipefail
ici="$(cd "$(dirname "$0")" && pwd)"
port="${PORT_HTTP:-18080}"
adresse="http://localhost:$port"
travail="$(mktemp -d)"

cat > "$travail/essai.env" <<FIN
DB_USER=proprefact
DB_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -hex 32)
APP_URL=$adresse
PORT_HTTP=$port
POSTGRES_VERSION=${POSTGRES_VERSION:-16}
FIN
if [ "${POSTGRES_VERSION:-16}" -ge 18 ]; then echo "POSTGRES_DONNEES=/var/lib/postgresql" >> "$travail/essai.env"; fi
pile=(docker compose -p proprefact-essai -f "$ici/docker-compose.yml" --env-file "$travail/essai.env")

terminer() {
  local code=$?
  if [ "$code" -ne 0 ]; then
    echo "== ÉCHEC : journal de l'application"
    "${pile[@]}" logs app | tail -60 || true
  fi
  "${pile[@]}" down -v --remove-orphans > /dev/null 2>&1 || true
  rm -rf "$travail"
  exit "$code"
}
trap terminer EXIT

echec() { echo "ÉCHEC : $*" >&2; exit 1; }
etape() { echo "ok  $*"; }
# Code HTTP d'une requête ; le corps est gardé dans $travail/corps, les en-têtes dans $travail/entetes.
http() { curl -s -o "$travail/corps" -D "$travail/entetes" -w '%{http_code}' "$@"; }
champ() { jq -r "$1" "$travail/corps"; }

if [ -n "${SANS_CONSTRUCTION:-}" ]; then
  "${pile[@]}" up -d --no-build
else
  "${pile[@]}" up -d --build
fi

echo "== Attente du démarrage (migrations comprises)"
for _ in $(seq 1 90); do
  if [ "$(http "$adresse/actuator/health")" = 200 ] && [ "$(champ .status)" = UP ]; then break; fi
  sleep 2
done
[ "$(champ .status)" = UP ] || echec "l'application ne répond pas UP sur /actuator/health"
etape "santé : UP"

[ "$(http "$adresse/")" = 200 ] && grep -q "<app-root" "$travail/corps" || echec "accueil"
etape "accueil : 200, page de l'application"
[ "$(http "$adresse/factures")" = 200 ] && grep -q "<app-root" "$travail/corps" || echec "/factures"
grep -qi "^cache-control: no-cache" "$travail/entetes" || echec "/factures sans Cache-Control: no-cache"
etape "/factures : 200, page de l'application, no-cache"
script="$(grep -o 'main-[A-Za-z0-9_-]*\.js' "$travail/corps" | head -1)"
[ "$(http "$adresse/$script")" = 200 ] && grep -qi "^cache-control:.*immutable" "$travail/entetes" \
  || echec "$script"
etape "$script : 200, en cache longtemps"
[ "$(http "$adresse/api/clients")" = 401 ] || echec "/api/clients sans jeton devrait répondre 401"
etape "/api/clients sans jeton : 401"

email="essai-$(date +%s)@exemple.fr"
code=$(http -X POST "$adresse/api/auth/inscription" -H 'Content-Type: application/json' -d @- <<FIN
{"email":"$email","motDePasse":"mot-de-passe-solide","raisonSociale":"Essai Propreté SARL","siren":"552100554",
 "tvaIntra":"FR00552100554","adresse":"5 rue des Lilas","codePostal":"69003","ville":"Lyon",
 "iban":"FR7630006000011234567890189","bic":"AGRIFRPP"}
FIN
)
[ "$code" = 202 ] || echec "inscription : $code"
etape "inscription : 202"

lien=""
for _ in $(seq 1 15); do
  lien="$("${pile[@]}" logs app 2>&1 | grep -o "$adresse/confirmation?jeton=[A-Za-z0-9_-]*" | tail -1 || true)"
  [ -n "$lien" ] && break
  sleep 1
done
[ -n "$lien" ] || echec "lien de confirmation absent du journal"
etape "lien de confirmation dans le journal : $lien"

[ "$(http "$lien")" = 200 ] && grep -q "<app-root" "$travail/corps" || echec "le lien n'ouvre pas l'application"
etape "le lien ouvre l'application"
code=$(http -X POST "$adresse/api/auth/confirmation" -H 'Content-Type: application/json' \
  -d "{\"jeton\":\"${lien##*jeton=}\"}")
[ "$code" = 200 ] || echec "confirmation : $code"
jeton="$(champ .jeton)"
etape "confirmation : 200, connecté"

api() { # méthode chemin [json] : la requête doit réussir (200)
  local code
  code=$(http -X "$1" "$adresse$2" -H "Authorization: Bearer $jeton" -H 'Content-Type: application/json' \
    ${3:+-d "$3"})
  [ "$code" = 200 ] || echec "$1 $2 : $code $(cat "$travail/corps")"
}
api POST /api/clients '{"raisonSociale":"Cabinet Durand","siren":"732829320","tvaIntra":"FR44732829320",
  "adresse":"12 rue de la Paix","codePostal":"69002","ville":"Lyon"}'
client="$(champ .id)"
api POST "/api/clients/$client/sites" '{"nom":"Bureaux","adresse":"12 rue de la Paix","codePostal":"69002",
  "ville":"Lyon"}'
site="$(champ .id)"
api POST /api/contrats "{\"siteId\":$site,\"libelle\":\"Entretien des bureaux\",\"montantMensuelHt\":480.00,
  \"tauxTva\":20,\"dateDebut\":\"2026-01-01\"}"
etape "client, site et contrat créés"
api POST "/api/factures/generer-mensuelles?mois=2026-11"
facture="$(champ '.[0].id')"
[ "$(champ '.[0].totalTtc == 576')" = true ] || echec "total TTC inattendu : $(champ '.[0].totalTtc')"
api POST "/api/factures/$facture/valider"
numero="$(champ .numero)"
[[ "$numero" =~ ^F-[0-9]{4}-00001$ ]] || echec "numéro inattendu : $numero"
etape "facture validée : $numero, 576,00 € TTC"

code=$(curl -s -o "$travail/facture.pdf" -w '%{http_code}' -H "Authorization: Bearer $jeton" \
  "$adresse/api/factures/$facture/facturx")
[ "$code" = 200 ] && [ "$(head -c 4 "$travail/facture.pdf")" = "%PDF" ] || echec "Factur-X : $code"
grep -aq "factur-x.xml" "$travail/facture.pdf" || echec "le PDF n'embarque pas factur-x.xml"
etape "Factur-X téléchargé : PDF de $(wc -c < "$travail/facture.pdf") octets, factur-x.xml embarqué"

echo "Essai réussi."
