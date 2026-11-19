# Image unique de ProPreFact : Spring Boot sert l'API et le front-end Angular à la même adresse.
# Construite par Railway à chaque mise à jour de la branche (voir DEPLOIEMENT-RAILWAY.md) et par l'intégration
# continue. Les tests ne sont pas relancés ici : ils tournent dans l'intégration continue, avec PostgreSQL.
# Aucun secret n'est nécessaire à la construction : ils ne servent qu'au lancement (variables d'environnement).

# 1. Front-end : compilation Angular.
FROM node:22-bookworm-slim AS front
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npx ng build

# 2. Back-end : jar Spring Boot, avec le front compilé dans static/ (voir <resources> dans backend/pom.xml).
FROM eclipse-temurin:21-jdk AS back
WORKDIR /src/backend
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
COPY backend/src src
COPY --from=front /src/frontend/dist/frontend/browser /src/frontend/dist/frontend/browser
RUN ./mvnw -B -ntp -Dmaven.test.skip=true package \
    && cp target/proprefact-backend-*.jar /src/app.jar

# 3. Image finale : seulement Java et le jar.
FROM eclipse-temurin:21-jre

# L'application ne tourne pas en root.
RUN useradd --system --uid 10001 --no-create-home proprefact
WORKDIR /app
COPY --from=back /src/app.jar app.jar
USER proprefact

ENV TZ=Europe/Paris
# Port d'écoute : variable PORT si l'hébergeur la fournit (Railway le fait), sinon 8080.
EXPOSE 8080
# Le conteneur dispose de la mémoire que lui donne l'hébergeur : la JVM en prend 75 %.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
