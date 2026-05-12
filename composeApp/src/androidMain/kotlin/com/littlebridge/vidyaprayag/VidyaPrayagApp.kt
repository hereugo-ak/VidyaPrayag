package com.littlebridge.vidyaprayag

import android.app.Application
import com.littlebridge.vidyaprayag.di.initKoin
import org.koin.android.ext.koin.androidContext

class VidyaPrayagApp : Application() {
    companion object {
        lateinit var instance: VidyaPrayagApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        initKoin {
            androidContext(this@VidyaPrayagApp)
        }
    }
}
