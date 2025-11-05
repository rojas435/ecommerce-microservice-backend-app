# Multi-stage Dockerfile for payment-service
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy full repository into the build context so the parent POM and modules are available
COPY . .

# Download dependencies for the specific module
RUN mvn -f payment-service/pom.xml dependency:go-offline -B

# Build the payment-service module (skip tests - they run in the pipeline earlier)
RUN mvn -f payment-service/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /build/payment-service/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
EXPOSE 8400
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8400/actuator/health || exit 1
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

