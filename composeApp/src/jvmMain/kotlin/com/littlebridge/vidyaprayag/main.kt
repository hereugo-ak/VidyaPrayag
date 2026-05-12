package com.littlebridge.vidyaprayag

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.littlebridge.vidyaprayag.di.initKoin

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Vidya Prayag",
        ) {
            App()
        }
    }
}
