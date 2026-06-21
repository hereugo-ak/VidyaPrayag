rootProject.name = "VidyaPrayag"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// -----------------------------------------------------------------------------
// Project includes
// -----------------------------------------------------------------------------
// `:server` is a pure JVM Ktor backend that has been made fully self-contained
// (see comment in `server/build.gradle.kts`). When you only want to build/run
// the backend — e.g. for local API testing in Postman — you can skip
// configuring the heavy Kotlin Multiplatform modules (`:composeApp`, `:shared`,
// which pull in Android/iOS/JS/wasm + Compose MP + Room/KSP) by passing
// `-Pserver-only=true` (or `-PserverOnly=true`) to Gradle:
//
//   Windows CMD : gradlew.bat :server:run -Pserver-only=true
//   Linux/macOS : ./gradlew :server:run -Pserver-only=true
//
// This dramatically reduces cold-clone build time (no AGP/Compose/KMP/KSP
// downloads or configuration). For full multi-module work (mobile/web apps),
// simply omit the flag and all modules will be included as before.
val serverOnly: Boolean =
    (settings.providers.gradleProperty("server-only").orNull
        ?: settings.providers.gradleProperty("serverOnly").orNull)
        ?.toBoolean() == true

include(":server")
if (!serverOnly) {
    include(":composeApp")
    include(":shared")
}