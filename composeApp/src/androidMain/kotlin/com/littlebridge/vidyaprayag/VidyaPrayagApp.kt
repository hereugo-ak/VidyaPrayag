package com.littlebridge.vidyaprayag

import android.app.Application
import com.littlebridge.vidyaprayag.di.initKoin
import com.littlebridge.vidyaprayag.util.Environment
import org.koin.android.ext.koin.androidContext

class VidyaPrayagApp : Application() {
    companion object {
        lateinit var instance: VidyaPrayagApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        val env = try {
            Environment.valueOf(BuildConfig.ENVIRONMENT)
        } catch (e: Exception) {
            Environment.DEV
        }

        initKoin(environment = env) {
            androidContext(this@VidyaPrayagApp)
        }
    }
}
