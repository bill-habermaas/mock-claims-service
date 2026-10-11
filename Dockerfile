# Build stage: compile and package with Maven.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Run stage: JRE only, non-root user.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /build/target/claims-migration-service-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
