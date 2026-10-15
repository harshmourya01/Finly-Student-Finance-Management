# Build the Spring Boot executable JAR with the project's Java 21 target.
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package \
    && cp target/student-finance-backend-0.0.1-SNAPSHOT.jar /workspace/app.jar

# Run on a small Java 21 runtime image as a non-root user.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /workspace/app.jar /app/app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
