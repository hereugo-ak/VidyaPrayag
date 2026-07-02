package com.littlebridge.enrollplus

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.littlebridge.enrollplus.di.initKoin
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.darwin.NSObject

fun MainViewController() = ComposeUIViewController {
    initKoin()

    var deepLink by remember { mutableStateOf<String?>(readDeepLinkFromDefaults()) }

    LaunchedEffect(Unit) {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = "DeepLinkReceived",
            `object` = null,
            queue = null,
        ) { _ ->
            deepLink = readDeepLinkFromDefaults()
        }
    }

    App(
        deepLink = deepLink,
        onDeepLinkConsumed = {
            NSUserDefaults.standard.removeObjectForKey("deepLinkUrl")
            deepLink = null
        },
    )
}

private fun readDeepLinkFromDefaults(): String? {
    val raw = NSUserDefaults.standard.stringForKey("deepLinkUrl") ?: return null
    // Convert vidyaprayag://app/<path> → /<path> for the nav layer.
    val prefix = "vidyaprayag://app"
    return when {
        raw.startsWith(prefix) -> raw.substring(prefix.length).ifBlank { null }
        raw.startsWith("/") -> raw
        else -> null
    }
}