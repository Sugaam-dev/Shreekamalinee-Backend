# ==========================================
# Stage 1: Build the JAR inside the container
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy the pom.xml and download dependencies (utilizing Docker layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and compile the jar package
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Runtime image (lightweight JRE)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the compiled JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]