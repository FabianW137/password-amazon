# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
COPY src ./src
COPY public ./public
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV PORT=8080
EXPOSE 8080
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/public /app/public
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]
