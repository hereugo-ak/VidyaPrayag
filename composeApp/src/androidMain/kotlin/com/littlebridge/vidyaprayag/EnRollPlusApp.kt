package com.littlebridge.vidyaprayag

import android.app.Application
import android.util.Log
import com.littlebridge.vidyaprayag.di.initKoin
import com.littlebridge.vidyaprayag.util.AppConfig
import org.koin.android.ext.koin.androidContext

class EnRollPlusApp : Application() {
    companion object {
        lateinit var instance: EnRollPlusApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Surface the resolved backend at boot so you can confirm in Logcat
        // exactly which server the phone is calling (filter tag: VidyaPrayagApp).
        Log.i(
            "VidyaPrayagApp",
            "Backend -> authBaseUrl=${AppConfig.authBaseUrl} schoolBaseUrl=${AppConfig.schoolBaseUrl}"
        )

        initKoin {
            androidContext(this@EnRollPlusApp)
        }
    }
}
