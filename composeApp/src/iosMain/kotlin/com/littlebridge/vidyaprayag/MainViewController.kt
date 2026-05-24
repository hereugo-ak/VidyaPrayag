package com.littlebridge.vidyaprayag

import androidx.compose.ui.window.ComposeUIViewController
import com.littlebridge.vidyaprayag.di.initKoin

fun MainViewController() = ComposeUIViewController { 
    // Initialize Koin for iOS
    initKoin()
    App() 
}