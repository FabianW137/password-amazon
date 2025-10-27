# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1) Dependencies cachen über Layers (ohne BuildKit)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# 2) Quellcode kopieren und bauen
COPY src ./src
RUN mvn -q -DskipTests package

# --- Runtime-Image ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# nimm das erzeugte Jar (Name ggf. anpassen)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]


# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV PORT=8080
EXPOSE 8080
COPY --from=build /app/target/*.jar app.jar
CMD ["sh","-c","java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]
