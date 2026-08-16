# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------
# Build Stage using JDK on Alpine
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copy build files and Gradle wrapper
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/

# Download and cache dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code and package application
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ------------------------------------------------------------------------------
# Production Runtime Stage using lightweight JRE on Alpine
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create a non-privileged user for enhanced security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy compiled artifact from build stage
COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
