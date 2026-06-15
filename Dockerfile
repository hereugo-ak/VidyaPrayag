# =========================
# BUILD STAGE
# =========================

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

# Build install distribution for server.
#
# IMPORTANT: We pass `-Pserver-only=true` so settings.gradle.kts skips including
# the `:composeApp` and `:shared` Kotlin Multiplatform modules. Those modules
# pull in AGP 9.x (beta), Compose Multiplatform, Room/KSP and Kotlin/Native
# (iOS targets) which:
#   - Download ~6+ GB of toolchain artifacts on a cold build (huge on Render).
#   - Trigger a Kotlin/Native version-mismatch warning (Native 2.1.0 vs Kotlin
#     2.2.10) that can stall configuration of the :shared module.
#   - Are NOT required to build the Ktor backend — `:server` is a pure JVM
#     module with no `projects.shared` dependency (see commit 93b87e8).
#
# With this flag the Render build configures only `:server` and finishes in a
# few minutes instead of timing out during ":shared" configuration.
#
# `--no-daemon` keeps Gradle from leaving a long-lived JVM behind in the
# Docker layer (recommended for CI/containers).
# `kotlin.native.ignoreDisabledTargets=true` silences the iOS-target warning
# (defence-in-depth, since we shouldn't even be configuring :shared here).
RUN ./gradlew :server:installDist \
        -Pserver-only=true \
        -Pkotlin.native.ignoreDisabledTargets=true \
        --no-daemon \
        --stacktrace


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