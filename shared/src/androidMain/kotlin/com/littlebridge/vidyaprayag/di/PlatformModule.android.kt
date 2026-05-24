package com.littlebridge.vidyaprayag.di

import com.littlebridge.vidyaprayag.AndroidPlatform
import com.littlebridge.vidyaprayag.Platform
import com.littlebridge.vidyaprayag.core.database.AppDatabase
import com.littlebridge.vidyaprayag.core.database.DatabaseFactory
import com.littlebridge.vidyaprayag.core.prefs.PreferenceManager
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.core.prefs.createDataStore
import com.littlebridge.vidyaprayag.feature.schools.data.local.RoomSchoolLocalDataSource
import com.littlebridge.vidyaprayag.feature.schools.data.local.SchoolLocalDataSource
import io.ktor.client.engine.okhttp.*
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<Platform> { AndroidPlatform(androidContext().cacheDir.absolutePath.toPath()) }
    single { OkHttp.create() }
    single { DatabaseFactory(androidContext()) }
    single<AppDatabase> { get<DatabaseFactory>().createBuilder().build() }
    single { get<AppDatabase>().schoolDao() }
    single<SchoolLocalDataSource> { RoomSchoolLocalDataSource(get()) }

    single {
        val context = androidContext()
        createDataStore {
            context.filesDir.resolve("edu_trust_prefs.preferences_pb").absolutePath
        }
    }
    single<PreferenceRepository> { PreferenceManager(get()) }
}
