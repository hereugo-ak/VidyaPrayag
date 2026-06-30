package com.littlebridge.enrollplus.core.offline.sync

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()
