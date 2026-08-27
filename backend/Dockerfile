# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------
# Base Stage: JDK 25 on Alpine & Gradle Dependencies Caching
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS base

WORKDIR /app

# Install bash and curl for dev tooling and health probes
RUN apk add --no-cache bash curl

# Copy build descriptors & Gradle wrapper first for cache optimization
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/

# Cache external dependencies in image layer
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# ------------------------------------------------------------------------------
# Target: Development (docker build --target dev -t backend:dev .)
# ------------------------------------------------------------------------------
FROM base AS dev

# Copy source code (can also be mounted via docker-compose volume for hot reload)
COPY src/ src/

EXPOSE 8080 5005

ENV SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

ENTRYPOINT ["./gradlew", "bootRun", "--no-daemon"]

# ------------------------------------------------------------------------------
# Stage: Builder (Compiles JAR & Extracts Spring Boot Layers)
# ------------------------------------------------------------------------------
FROM base AS builder

COPY src/ src/

# Build executable bootJar
RUN ./gradlew bootJar --no-daemon -x test

# Extract Spring Boot Layered JAR for optimal Docker cache reuse
RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination /build/extracted

# ------------------------------------------------------------------------------
# Target: Production (Default: docker build --target prod -t backend:prod .)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS prod

WORKDIR /app

# Install curl for container health check
RUN apk add --no-cache curl

# Create non-root system user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy extracted layers in order of change frequency
COPY --from=builder --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /build/extracted/application/ ./

USER spring:spring

EXPOSE 8080

# Production JVM optimizations for containerized environments
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
