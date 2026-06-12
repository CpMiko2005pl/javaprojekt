# ===== Etap 1: budowanie jara =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# najpierw same zaleznosci (cache warstwy Dockera)
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ===== Etap 2: lekki runtime =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render ustawia PORT; application.properties czyta server.port=${PORT:8080}
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
