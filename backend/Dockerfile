# syntax=docker/dockerfile:1

# Construction — le wrapper est utilisé ici aussi : la version de Maven qui
# construit l'image est celle que le dépôt déclare (contrainte B1).
FROM eclipse-temurin:17-jdk AS construction
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

# Le dépôt Maven est monté en cache BuildKit plutôt que copié dans une couche :
# la première construction télécharge, les suivantes ne retéléchargent rien.
# On évite volontairement `dependency:go-offline`, qui résout bien plus que ce
# dont la construction a besoin et triple la durée du premier build.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp clean package -DskipTests

# Exécution — JRE seul, pas de JDK ni de sources dans l'image finale.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=construction /build/target/*.jar application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
