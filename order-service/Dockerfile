# Multi-stage Dockerfile for order-service
# Stage 1: Build with Maven
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy only pom.xml first (better layer caching)
COPY pom.xml .

# Download dependencies (cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests - they run in CI/CD)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime with JRE only (smaller image)
FROM eclipse-temurin:17-jre-alpine

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

USER spring:spring

# Expose port
EXPOSE 8300

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8300/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

