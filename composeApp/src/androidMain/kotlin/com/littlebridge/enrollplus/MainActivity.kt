package com.littlebridge.enrollplus

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.littlebridge.enrollplus.notification.NotificationManagerHelper

class MainActivity : ComponentActivity() {
    private val contentReady = mutableStateOf(false)
    private val deepLink = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        enableEdgeToEdge(
            statusBarStyle = if (isDarkMode) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )

            },
            navigationBarStyle = if (isDarkMode) {
                SystemBarStyle.dark(
                    android.graphics.Color.TRANSPARENT
                )
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )
        splashScreen.setKeepOnScreenCondition { !contentReady.value }

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val iconView = try {
                splashScreenViewProvider.iconView
            } catch (_: Exception) {
                null
            }

            val targetView = iconView ?: splashScreenViewProvider.view
            val fade = ObjectAnimator.ofFloat(targetView, View.ALPHA, 1f, 0f)
            val scaleX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 1f, 1.12f)
            val scaleY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 1f, 1.12f)

            listOf(fade, scaleX, scaleY).forEach {
                it.interpolator = AnticipateInterpolator()
                it.duration = 280L
            }

            fade.doOnEnd { splashScreenViewProvider.remove() }

            fade.start()
            scaleX.start()
            scaleY.start()
        }

        // Read deep link from notification intent (if launched from a push tap).
        deepLink.value = extractDeepLink(intent)

        setContent {
            App(
                onContentRendered = { contentReady.value = true },
                deepLink = deepLink.value,
                onDeepLinkConsumed = { deepLink.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLink.value = extractDeepLink(intent)
    }

    private fun extractDeepLink(intent: Intent): String? {
        // 1. Notification tap: raw path stored as extra by NotificationManagerHelper.
        if (intent.getBooleanExtra(NotificationManagerHelper.EXTRA_FROM_PUSH, false)) {
            return intent.getStringExtra(NotificationManagerHelper.EXTRA_DEEP_LINK)
        }
        // 2. External deep link: vidyaprayag://app/<path> URI from the intent-filter.
        val data = intent.data
        if (data != null && data.scheme == NotificationManagerHelper.DEEP_LINK_SCHEME && data.host == "app") {
            val path = data.path
            return if (!path.isNullOrBlank()) path else null
        }
        return null
    }
}
