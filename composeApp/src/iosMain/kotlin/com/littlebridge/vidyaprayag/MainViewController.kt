package com.littlebridge.vidyaprayag

import androidx.compose.ui.window.ComposeUIViewController
import com.littlebridge.vidyaprayag.di.initKoin
import com.littlebridge.vidyaprayag.util.Environment

fun MainViewController() = ComposeUIViewController { 
    // Initialize Koin for iOS
    initKoin(environment = Environment.STAGING)
    App() 
}