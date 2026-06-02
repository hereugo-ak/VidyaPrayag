import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization")
}

// ---------------------------------------------------------------------------
// local.properties — read devBaseUrl so the dev flavor can point at the
// laptop's local server (http://192.168.1.9:8080) when phone + laptop are on
// the same WiFi.  Falls back to render.com if the key is absent.
// Add this to your local.properties (not committed to git):
//   devBaseUrl=http://192.168.1.9:8080
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
    
    iosArm64()
    iosSimulatorArm64()
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        val roomMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.datastore.preferences)
            }
        }
        
        androidMain.get().dependsOn(roomMain)
        jvmMain.get().dependsOn(roomMain)
        
        iosArm64Main.get().dependsOn(roomMain)
        iosSimulatorArm64Main.get().dependsOn(roomMain)

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.okio)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.littlebridge.vidyaprayag.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}


// room {
//     schemaDirectory("/tmp/schemas")
// }
