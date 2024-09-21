# Use an official Maven image to build the jar file
FROM maven:3.9-eclipse-temurin-22-alpine AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code into the container
COPY src ./src

# Build the jar file
RUN mvn clean package -DskipTests

# Use an official Eclipse Temurin runtime as a parent image
FROM eclipse-temurin:22-jre-alpine

# Create a non-root user and group
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Set the working directory in the container
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/rentbot-0.0.1-SNAPSHOT.jar /app/rentbot.jar

# Expose the port the application runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "rentbot.jar"]