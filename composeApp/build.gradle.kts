import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization")
}

// ---------------------------------------------------------------------------
// local.properties — devBaseUrl lets you point the dev flavor at your laptop
// (http://192.168.1.9:8080) while on the same WiFi as your phone.
// Falls back to render.com when the key is absent.
// ---------------------------------------------------------------------------
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val renderFallbackUrl = "https://vidyaprayag-1.onrender.com"
val devBaseUrl: String = localProps.getProperty("devBaseUrl")?.takeIf { it.isNotBlank() }
    ?: renderFallbackUrl

// Make the resolved dev URL visible at configuration time so you never have to
// guess which backend the phone is hitting.  If it fell back to render while
// you expected your laptop, this warning tells you `devBaseUrl` is missing.
if (devBaseUrl == renderFallbackUrl) {
    logger.warn(
        "[VidyaPrayag] devBaseUrl NOT set in local.properties -> dev flavor will " +
            "use $renderFallbackUrl. To point the phone at your laptop, add " +
            "`devBaseUrl=http://<laptop-LAN-ip>:8080` to local.properties."
    )
} else {
    logger.lifecycle("[VidyaPrayag] dev flavor baseUrl = $devBaseUrl")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.compose)
            implementation("io.insert-koin:koin-compose-viewmodel:4.0.0")
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.okio)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(libs.androidx.navigation.compose)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "com.littlebridge.vidyaprayag"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.littlebridge.vidyaprayag"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    
    buildFeatures {
        buildConfig = true
    }
    
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "AUTH_BASE_URL", "\"$devBaseUrl\"")
            buildConfigField("String", "SCHOOL_BASE_URL", "\"$devBaseUrl\"")
        }
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "AUTH_BASE_URL", "\"https://vidyaprayag-1.onrender.com\"")
            buildConfigField("String", "SCHOOL_BASE_URL", "\"https://vidyaprayag-1.onrender.com\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "AUTH_BASE_URL", "\"https://vidyaprayag-1.onrender.com\"")
            buildConfigField("String", "SCHOOL_BASE_URL", "\"https://vidyaprayag-1.onrender.com\"")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.littlebridge.vidyaprayag.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.littlebridge.vidyaprayag"
            packageVersion = "1.0.0"
        }
    }
}
