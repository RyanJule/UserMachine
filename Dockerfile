# Use OpenJDK as base image
FROM openjdk:17-jdk-slim

# Set working directory in the container
WORKDIR /app

# Copy the built jar file into the container
COPY target/UserMachine-0.0.1-SNAPSHOT.jar /app/myapp.jar

# Expose the application port
EXPOSE 8080

# Run the Spring Boot app
CMD ["java", "-jar", "/app/myapp.jar"]
