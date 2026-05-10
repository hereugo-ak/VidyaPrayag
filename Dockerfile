# Use an official JDK image as the base for building
FROM openjdk:21-slim AS build

# Set the working directory
WORKDIR /app

# Copy the entire project
COPY . .

# Grant execution permissions to gradlew
RUN chmod +x ./gradlew

# Build the shadowJar for the server module
RUN ./gradlew :server:shadowJar --no-daemon

# Use a slim JRE image for the final run stage
FROM openjdk:21-slim

# Set the working directory
WORKDIR /app

# Copy the built shadowJar from the build stage
COPY --from=build /app/server/build/libs/server-all.jar app.jar

# The server port is configurable via the PORT environment variable (default 8080)
ENV PORT=8080
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]
