package com.littlebridge.vidyaprayag.util

enum class Environment(val baseUrl: String) {
    DEV("http://10.0.2.2:8080"),
    STAGING("https://vidyaprayag-1.onrender.com"),
    PRODUCTION("https://vidyaprayag-1.onrender.com")
}

object AppConfig {
    var current: Environment = Environment.DEV
}
