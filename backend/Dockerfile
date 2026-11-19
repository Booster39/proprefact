# Image du back-end. Le jar est construit avant, par ../deploiement/construire.sh (./mvnw verify) :
# l'image ne télécharge rien et ne contient ni Maven ni le code source.
FROM eclipse-temurin:21-jre

# L'application ne tourne pas en root.
RUN useradd --system --uid 10001 --no-create-home proprefact
WORKDIR /app
COPY target/proprefact-backend-*.jar app.jar
USER proprefact

EXPOSE 8080
# Le conteneur dispose de la mémoire que lui donne l'hébergeur : la JVM en prend 75 %.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
