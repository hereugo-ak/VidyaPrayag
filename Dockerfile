# =========================
# BUILD STAGE
# =========================

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

# Build install distribution for server
RUN ./gradlew :server:installDist --no-daemon


# =========================
# RUNTIME STAGE
# =========================

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy generated distribution
COPY --from=build /app/server/build/install/server /app/server

# Render provides PORT dynamically
ENV PORT=8080

EXPOSE 8080

# Run Ktor server
CMD ["./server/bin/server"]