# Image du front-end : nginx sert l'application Angular déjà compilée (npx ng build, lancé par
# ../deploiement/construire.sh) et relaie /api vers le back-end.
FROM nginx:1.29-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
