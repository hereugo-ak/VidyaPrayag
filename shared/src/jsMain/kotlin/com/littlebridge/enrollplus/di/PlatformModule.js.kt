package com.littlebridge.enrollplus.di

import com.littlebridge.enrollplus.core.connectivity.JsNetworkMonitor
import com.littlebridge.enrollplus.core.connectivity.NetworkMonitor
import com.littlebridge.enrollplus.core.offline.outbox.InMemoryOutboxRepository
import com.littlebridge.enrollplus.core.offline.outbox.OutboxRepository
import com.littlebridge.enrollplus.feature.admin.data.local.AnnouncementLocalDataSource
import com.littlebridge.enrollplus.feature.admin.data.local.InMemoryAnnouncementLocalDataSource
import com.littlebridge.enrollplus.feature.teacher.data.local.InMemoryTeacherDayLocalDataSource
import com.littlebridge.enrollplus.feature.teacher.data.local.TeacherDayLocalDataSource
import com.littlebridge.enrollplus.core.prefs.LocalStoragePreferenceManager
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.schools.data.local.InMemorySchoolLocalDataSource
import com.littlebridge.enrollplus.feature.schools.data.local.SchoolLocalDataSource
import io.ktor.client.engine.js.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<com.littlebridge.enrollplus.Platform> { com.littlebridge.enrollplus.getPlatform() }
    single { Js.create() }
    single<SchoolLocalDataSource> { InMemorySchoolLocalDataSource() }
    single<OutboxRepository> { InMemoryOutboxRepository() }
    single<AnnouncementLocalDataSource> { InMemoryAnnouncementLocalDataSource() }
    single<TeacherDayLocalDataSource> { InMemoryTeacherDayLocalDataSource() }
    single<PreferenceRepository> { LocalStoragePreferenceManager() }
    single<NetworkMonitor> { JsNetworkMonitor() }
}
