# Dockerfile for sistemareportes microservice
FROM eclipse-temurin:17

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application and run tests
RUN ./mvnw clean package

# Expose port
EXPOSE 8082

# Run the application
CMD ["java", "-jar", "target/sistemareportes-0.0.1-SNAPSHOT.jar"]
