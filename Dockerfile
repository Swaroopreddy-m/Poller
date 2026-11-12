# Multi-stage Dockerfile optimized for Render & Spring Boot 3 / Java 21
# Stage 1: Build & Package
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application without running tests during image assembly
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Distroless / Alpine Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root application user for production container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy built artifact
COPY --from=builder /workspace/target/communication-platform-*.jar app.jar

# JVM Performance & Container Memory Flags
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
