# Stage 1: Build stage with OpenJDK 17 and Maven
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy Maven Wrapper and POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal runtime image with Eclipse Temurin JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the executable fat jar from builder stage
COPY --from=builder /app/target/campus-event-system-1.0.0.jar app.jar

# Dynamic port environment variable
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot with urandom for fast secure startup
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
