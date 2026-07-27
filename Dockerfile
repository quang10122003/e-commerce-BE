
# Stage 1: Build Spring Boot application
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

# Tải toàn bộ dependency
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src
# Build 
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Chọn profile 
ENV SPRING_PROFILES_ACTIVE=docker

COPY --from=builder /app/target/*.jar app.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]