
# Stage 1: Build the Java application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy project configuration and source code
COPY pom.xml .
COPY src ./src

# Compile, test and package the application
RUN mvn -B clean package

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the generated JAR from the build stage
COPY --from=build /app/target/taskapi-0.0.1-SNAPSHOT.jar app.jar

# Application listens on port 8081
EXPOSE 8081

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
