# ==============================================================================
# CampusFix Backend — Multi-stage Production Dockerfile
# Base: Eclipse Temurin Java 17
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build the Spring Boot executable jar with Maven
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache Maven dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Build production jar skipping test execution (tests run during CI/CD)
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal, secure JRE runtime container
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Install curl for Docker healthchecks and create non-root service user
RUN apk add --no-cache curl \
    && addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for complaint upload attachments and configure ownership
RUN mkdir -p /app/uploads/complaints && chown -R appuser:appgroup /app

# Copy built artifact from builder stage
COPY --from=builder /build/target/*.jar /app/app.jar
RUN chown appuser:appgroup /app/app.jar

# Switch to non-root user for security hardening
USER appuser

# Expose Spring Boot server port
EXPOSE 8080

# Environment variable defaults (overridden by docker-compose or Kubernetes)
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Container healthcheck via backend actuator/health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
