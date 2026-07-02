@file:OptIn(androidx.annotation.OptIn::class)

package com.littlebridge.enrollplus.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable

actual fun hapticFeedback(type: HapticType) {
    // No-op on Android without context — use hapticFeedbackComposable() instead
}

@Composable
fun hapticFeedbackComposable(type: HapticType) {
    val context = LocalContext.current
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        when (type) {
            HapticType.SUCCESS -> vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
            HapticType.ERROR -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
            HapticType.WARNING -> vibrator.vibrate(VibrationEffect.createOneShot(30, 50))
            HapticType.SELECTION -> vibrator.vibrate(VibrationEffect.createOneShot(10, 30))
        }
    } else {
        @Suppress("DEPRECATION")
        when (type) {
            HapticType.SUCCESS -> vibrator.vibrate(50)
            HapticType.ERROR -> vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
            HapticType.WARNING -> vibrator.vibrate(30)
            HapticType.SELECTION -> vibrator.vibrate(10)
        }
    }
}
