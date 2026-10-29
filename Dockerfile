# Build stage: compiles the jar, so the host needs no JDK or Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Run stage: only a Java runtime and the jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/customer-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
