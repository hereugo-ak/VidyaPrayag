package com.littlebridge.vidyaprayag.util

import kotlin.js.Date

actual fun todayIso(): String {
    val now = Date()
    val y = now.getFullYear()
    val m = now.getMonth() + 1 // JS month is 0-based
    val d = now.getDate()
    return isoOf(y, m, d)
}
