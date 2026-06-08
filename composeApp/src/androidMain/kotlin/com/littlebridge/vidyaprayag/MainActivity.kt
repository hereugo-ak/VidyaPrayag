package com.littlebridge.vidyaprayag

import android.animation.ObjectAnimator
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

class MainActivity : ComponentActivity() {

    // Flips true the moment Compose has laid out its first frame. The native
    // splash stays up until then (setKeepOnScreenCondition), so there is never
    // a blank/white window between the system splash and the first teal frame.
    private val contentReady = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // FEATURE 1: install the native Android 12+ SplashScreen API (compat back to
        // API 24) BEFORE super.onCreate() so the system splash window is taken over.
        val splashScreen = installSplashScreen()

        // Conditional status bar content color (RA-67): if the system is in dark mode,
        // use light icons (dark style); otherwise use dark icons (light style).
        // This satisfies "if background is light make content dark and vice versa".
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
            }
        )
        super.onCreate(savedInstanceState)

        // Hold the splash on screen until Compose draws its first frame — no
        // artificial timer (LAW 5 / FEATURE 1 "no artificial delay"). The actual
        // JWT+role session check then runs in parallel inside App()/MainViewModel,
        // showing SplashScreenV2 (also teal) so the hand-off has zero colour seam.
        splashScreen.setKeepOnScreenCondition { !contentReady.value }

        // Exit animation: the splash icon fades and scales up slightly as the first
        // screen enters, then the splash view is removed (FEATURE 1 requirement).
        splashScreen.setOnExitAnimationListener { provider ->
            val iconView = runCatching { provider.iconView }.getOrNull()

            if (iconView == null) {
                provider.remove()
                return@setOnExitAnimationListener
            }

            val fade = ObjectAnimator.ofFloat(iconView, View.ALPHA, 1f, 0f)
            val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.12f)
            val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.12f)

            listOf(fade, scaleX, scaleY).forEach {
                it.interpolator = AnticipateInterpolator()
                it.duration = 280L
            }

            fade.doOnEnd { provider.remove() }

            fade.start()
            scaleX.start()
            scaleY.start()
        }

        setContent {
            App(onContentRendered = { contentReady.value = true })
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
