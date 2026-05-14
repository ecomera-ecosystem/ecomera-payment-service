# ===========================
# STAGE 1 — BUILD
# ===========================
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy only the files needed for dependency resolution first (better layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Pre-download maven dependencies in advance (caching optimization)
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline -B

# Copy the source code (Avoiding cache break in each code updates)
COPY src src

# Build the application (jar file - skip tests)
RUN ./mvnw clean package -DskipTests -q

# ===========================
# STAGE 2 — RUNTIME
# ===========================
FROM eclipse-temurin:17-jre-alpine

LABEL version="0.1.0"
LABEL description="Ecomera Payment Service - E-commerce Platform"
LABEL maintainer="youssef.ammari.795@gmail.com"

# Create user to run app as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

# Expose the port the app runs on
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
