# Multi-stage build for optimized image
# Stage 1: Build the application
FROM eclipse-temurin:21 AS builder
WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copy Maven configuration files first (for better caching)
COPY pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests && \
    mv target/*.jar app.jar

# Stage 2: Runtime image
FROM eclipse-temurin:21
WORKDIR /app

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Copy the built JAR from builder stage
COPY --from=builder /app/app.jar ./app.jar

# Create a non-root user and set ownership
RUN groupadd -r spring && \
    useradd -r -g spring spring && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose the port the app runs on
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]