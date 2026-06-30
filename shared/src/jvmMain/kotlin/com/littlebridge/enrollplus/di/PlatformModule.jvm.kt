package com.littlebridge.enrollplus.di

import com.littlebridge.enrollplus.core.database.AppDatabase
import com.littlebridge.enrollplus.core.database.DatabaseFactory
import com.littlebridge.enrollplus.core.database.MIGRATION_1_2
import com.littlebridge.enrollplus.core.database.MIGRATION_2_3
import com.littlebridge.enrollplus.core.database.MIGRATION_3_4
import com.littlebridge.enrollplus.core.offline.outbox.OutboxDao
import com.littlebridge.enrollplus.core.offline.outbox.OutboxRepository
import com.littlebridge.enrollplus.core.offline.outbox.RoomOutboxRepository
import com.littlebridge.enrollplus.feature.admin.data.local.AnnouncementLocalDataSource
import com.littlebridge.enrollplus.feature.admin.data.local.RoomAnnouncementLocalDataSource
import com.littlebridge.enrollplus.feature.teacher.data.local.RoomTeacherDayLocalDataSource
import com.littlebridge.enrollplus.feature.teacher.data.local.TeacherDayLocalDataSource
import com.littlebridge.enrollplus.core.prefs.PreferenceManager
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.core.prefs.createDataStore
import com.littlebridge.enrollplus.core.connectivity.JvmNetworkMonitor
import com.littlebridge.enrollplus.core.connectivity.NetworkMonitor
import com.littlebridge.enrollplus.feature.schools.data.local.RoomSchoolLocalDataSource
import com.littlebridge.enrollplus.feature.schools.data.local.SchoolLocalDataSource
import io.ktor.client.engine.okhttp.*
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    single<com.littlebridge.enrollplus.Platform> { com.littlebridge.enrollplus.getPlatform() }
    single { OkHttp.create() }
    single { DatabaseFactory() }
    single<AppDatabase> {
        get<DatabaseFactory>().createBuilder()
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }
    single { get<AppDatabase>().schoolDao() }
    single { get<AppDatabase>().outboxDao() }
    single<OutboxRepository> { RoomOutboxRepository(get()) }
    single { get<AppDatabase>().announcementDao() }
    single<AnnouncementLocalDataSource> { RoomAnnouncementLocalDataSource(get()) }
    single { get<AppDatabase>().teacherDayCacheDao() }
    single<TeacherDayLocalDataSource> { RoomTeacherDayLocalDataSource(get()) }
    single<SchoolLocalDataSource> { RoomSchoolLocalDataSource(get()) }
    single<NetworkMonitor> { JvmNetworkMonitor() }

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
