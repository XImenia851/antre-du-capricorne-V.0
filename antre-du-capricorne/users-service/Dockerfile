# Le contexte de build est la RACINE du dépôt (voir docker-compose.yml).

# ---- Étape 1 : compilation ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY users-service/pom.xml users-service/pom.xml
COPY webapp/pom.xml webapp/pom.xml
COPY users-service/src users-service/src
RUN mvn -B -q -pl users-service -am -DskipTests package

# ---- Étape 2 : image d'exécution minimale ----
FROM eclipse-temurin:21-jre
# curl sert au healthcheck ; l'application tourne sous un utilisateur non privilégié.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 10001 --no-create-home appuser
WORKDIR /app
COPY --from=build /build/users-service/target/users-service.jar app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
