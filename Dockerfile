FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the shadow JAR
COPY build/libs/src-all.jar /app/app.jar

# Set the host to 0.0.0.0 to allow connections from outside the container
ENV HOST=0.0.0.0

# Expose the port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Run the application
CMD ["java", "-jar", "app.jar"]