# Multi-stage Dockerfile for order-service
# Stage 1: Build with Maven
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy full repository into the build context so the parent POM and modules are available
# Using full copy increases build layer size but ensures mvn can resolve multi-module structure
COPY . .

# Download dependencies for the specific module (use module pom so parent relative paths resolve)
RUN mvn -f order-service/pom.xml dependency:go-offline -B

# Build the order-service module with memory constraints for Java compiler
# -Dmaven.compiler.fork=false reduces memory by not forking separate compiler process
# Skip tests as they run in the pipeline earlier
RUN mvn -f order-service/pom.xml clean package -DskipTests -B \
    -Dmaven.compiler.fork=false \
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

# Stage 2: Runtime with JRE only (smaller image)
FROM eclipse-temurin:17-jre-alpine

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR from builder stage (module target path)
COPY --from=builder /build/order-service/target/*.jar app.jar

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

