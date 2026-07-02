package com.littlebridge.enrollplus.di

import com.littlebridge.enrollplus.core.database.AppDatabase
import com.littlebridge.enrollplus.core.database.DatabaseFactory
import com.littlebridge.enrollplus.core.prefs.PreferenceManager
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.core.prefs.createDataStore
import com.littlebridge.enrollplus.feature.schools.data.local.RoomSchoolLocalDataSource
import com.littlebridge.enrollplus.feature.schools.data.local.SchoolLocalDataSource
import com.littlebridge.enrollplus.feature.library.data.local.LibraryLocalDataSource
import com.littlebridge.enrollplus.feature.library.data.local.RoomLibraryLocalDataSource
import io.ktor.client.engine.okhttp.*
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    single<com.littlebridge.enrollplus.Platform> { com.littlebridge.enrollplus.getPlatform() }
    single { OkHttp.create() }
    single { DatabaseFactory() }
    single<AppDatabase> { get<DatabaseFactory>().createBuilder().build() }
    single { get<AppDatabase>().schoolDao() }
    single<SchoolLocalDataSource> { RoomSchoolLocalDataSource(get()) }
    single { get<AppDatabase>().libraryBookDao() }
    single { get<AppDatabase>().libraryCacheDao() }
    single { get<AppDatabase>().libraryPendingActionDao() }
    single<LibraryLocalDataSource> { RoomLibraryLocalDataSource(get(), get(), get()) }

    single {
        createDataStore {
            File(System.getProperty("java.io.tmpdir"), "edu_trust_prefs.preferences_pb").absolutePath
        }
    }
    single<PreferenceRepository> { PreferenceManager(get()) }
    single<com.littlebridge.enrollplus.feature.notification.domain.service.NotificationService> {
        com.littlebridge.enrollplus.notification.DesktopNotificationService()
    }
}
