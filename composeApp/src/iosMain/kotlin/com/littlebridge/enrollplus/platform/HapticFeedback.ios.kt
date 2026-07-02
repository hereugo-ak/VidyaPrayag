package com.littlebridge.enrollplus.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual fun hapticFeedback(type: HapticType) {
    val style = when (type) {
        HapticType.SUCCESS -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
        HapticType.ERROR -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
        HapticType.WARNING -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
        HapticType.SELECTION -> UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft
    }
    val generator = UIImpactFeedbackGenerator(style)
    generator.impactOccurred()
}
