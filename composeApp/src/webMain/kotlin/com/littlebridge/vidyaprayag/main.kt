package com.littlebridge.vidyaprayag

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.littlebridge.vidyaprayag.di.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    
    ComposeViewport {
        App()
    }
}
