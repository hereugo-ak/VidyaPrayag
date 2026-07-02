package com.littlebridge.enrollplus.platform

enum class HapticType { SUCCESS, ERROR, WARNING, SELECTION }

expect fun hapticFeedback(type: HapticType)
