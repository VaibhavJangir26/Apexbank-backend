# ==========================================
# Stage 1: Build Stage
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Package application (skipping unit tests for deployment)
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Production Runtime Stage
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root spring user for enhanced container security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy compiled JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Render assigns a dynamic PORT environment variable (defaults to 8600 locally)
ENV PORT=8600
EXPOSE ${PORT}

# Configure Spring Boot server port dynamically via -Dserver.port=${PORT}
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
