# =========================
# BUILD STAGE
# =========================

FROM eclipse-temurin:21-jdk AS build

# Set working directory
WORKDIR /app

# Copy complete KMP monorepo
COPY . .

# Give execute permission to gradlew
RUN chmod +x ./gradlew

# Build fat jar for server module
RUN ./gradlew :server:shadowJar --no-daemon


# =========================
# RUNTIME STAGE
# =========================

FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Copy generated fat jar from build stage
COPY --from=build /app/server/build/libs/server-all.jar app.jar

# Render provides PORT dynamically
ENV PORT=8080

# Expose application port
EXPOSE 8080

# Start Ktor server
CMD ["java", "-jar", "app.jar"]
