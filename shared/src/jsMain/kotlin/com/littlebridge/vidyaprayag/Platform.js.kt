package com.littlebridge.vidyaprayag

import okio.Path
import okio.Path.Companion.toPath

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
    override val cacheDir: Path = "/dev/null".toPath()
}

actual fun getPlatform(): Platform = JsPlatform()