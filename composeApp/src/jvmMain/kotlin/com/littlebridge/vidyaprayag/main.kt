package com.littlebridge.vidyaprayag

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.littlebridge.vidyaprayag.di.initKoin
import com.littlebridge.vidyaprayag.util.Environment

fun main() {
    // Determine environment from system property or default to DEV
    val envName = System.getProperty("environment") ?: "DEV"
    val env = try { Environment.valueOf(envName) } catch (e: Exception) { Environment.DEV }
    
    initKoin(environment = env)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Vidya Prayag",
        ) {
            App()
        }
    }
}
