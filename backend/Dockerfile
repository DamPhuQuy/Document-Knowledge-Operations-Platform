# syntax=docker/dockerfile:1

# Base: JDK 25 with cached Gradle dependencies
FROM eclipse-temurin:25-jdk-alpine AS base
WORKDIR /app
RUN apk add --no-cache bash curl

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Dev: BootRun with JDWP remote debugging (port 5005)
FROM base AS dev
COPY src/ src/
EXPOSE 8080 5005
ENV SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ENTRYPOINT ["./gradlew", "bootRun", "--no-daemon"]

# Builder: Compile and extract Spring Boot jar layers
FROM base AS builder
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test
RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination /build/extracted

# Prod: Minimal JRE 25 runtime with non-root user
FROM eclipse-temurin:25-jre-alpine AS prod
WORKDIR /app
RUN apk add --no-cache curl && addgroup -S spring && adduser -S spring -G spring

# Copy layered jars in order of modification frequency
COPY --from=builder --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /build/extracted/application/ ./

USER spring:spring
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
