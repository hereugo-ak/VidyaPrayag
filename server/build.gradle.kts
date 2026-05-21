plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

group = "com.littlebridge.vidyaprayag"
version = "1.0.0"
application {
    mainClass.set("com.littlebridge.vidyaprayag.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    // NOTE: `:server` no longer depends on `:shared`.
    //
    // The `:shared` module is a Kotlin Multiplatform module (Android + iOS +
    // JVM + JS + wasmJs, plus AGP, Compose Multiplatform, Room/KSP). Depending
    // on it forces a full KMP configuration on every Gradle build of `:server`,
    // which on a cold clone downloads gigabytes of artifacts and can take 30+
    // minutes on average hardware/internet. `:server` only used two trivial
    // symbols from `:shared` (SERVER_PORT, Greeting), which are now inlined in
    // `server/src/main/kotlin/com/littlebridge/vidyaprayag/ServerEntry.kt`.
    //
    // If you ever need to share more code between server and the mobile/web
    // apps, prefer creating a JVM-only sub-module (e.g. `:shared-jvm`) instead
    // of reintroducing the full multiplatform dependency here.
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation("io.ktor:ktor-server-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")
    implementation("io.ktor:ktor-server-status-pages:3.4.3")
    implementation("io.ktor:ktor-server-cors:3.4.3")
    implementation("io.ktor:ktor-server-auth:3.4.3")
    implementation("io.ktor:ktor-server-auth-jwt:3.4.3")
    implementation("io.ktor:ktor-server-call-logging:3.4.3")

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.postgres)
    implementation(libs.hikaricp)
    implementation(libs.sqlite)
    implementation(libs.dotenv)

    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
