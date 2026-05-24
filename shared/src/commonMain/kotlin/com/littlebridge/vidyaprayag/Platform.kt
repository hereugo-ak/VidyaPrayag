package com.littlebridge.vidyaprayag

import okio.Path

interface Platform {
    val name: String
    val cacheDir: Path
}

expect fun getPlatform(): Platform