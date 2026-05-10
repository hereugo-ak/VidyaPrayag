package com.littlebridge.vidyaprayag

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.littlebridge.vidyaprayag.di.initKoin
import com.littlebridge.vidyaprayag.util.Environment

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Default to STAGING for web for now
    initKoin(environment = Environment.STAGING)
    
    ComposeViewport {
        App()
    }
}
