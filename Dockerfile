# --- Stage 1: Build ---
FROM openjdk:17-jdk AS builder

WORKDIR /app

# Copy build files
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src ./src

# Make gradlew executable and build the project
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar -x test

# --- Stage 2: Run ---
FROM openjdk:17-jdk

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*-all.jar ktor-ECommerce-all.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "ktor-ECommerce-all.jar"]