package com.littlebridge.vidyaprayag.di

import com.littlebridge.vidyaprayag.core.database.AppDatabase
import com.littlebridge.vidyaprayag.core.database.DatabaseFactory
import com.littlebridge.vidyaprayag.core.prefs.PreferenceManager
import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.core.prefs.createDataStore
import com.littlebridge.vidyaprayag.feature.schools.data.local.RoomSchoolLocalDataSource
import com.littlebridge.vidyaprayag.feature.schools.data.local.SchoolLocalDataSource
import io.ktor.client.engine.okhttp.*
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    single<com.littlebridge.vidyaprayag.Platform> { com.littlebridge.vidyaprayag.getPlatform() }
    single { OkHttp.create() }
    single { DatabaseFactory() }
    single<AppDatabase> { get<DatabaseFactory>().createBuilder().build() }
    single { get<AppDatabase>().schoolDao() }
    single<SchoolLocalDataSource> { RoomSchoolLocalDataSource(get()) }

    single {
        createDataStore {
            File(System.getProperty("java.io.tmpdir"), "edu_trust_prefs.preferences_pb").absolutePath
        }
    }
    single<PreferenceRepository> { PreferenceManager(get()) }
}
