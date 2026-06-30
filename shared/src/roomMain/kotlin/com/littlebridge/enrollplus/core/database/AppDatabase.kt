package com.littlebridge.enrollplus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.littlebridge.enrollplus.core.offline.outbox.OutboxDao
import com.littlebridge.enrollplus.core.offline.outbox.OutboxOperationEntity
import com.littlebridge.enrollplus.feature.admin.data.local.AnnouncementDao
import com.littlebridge.enrollplus.feature.admin.data.local.AnnouncementEntity
import com.littlebridge.enrollplus.feature.schools.data.local.SchoolDao
import com.littlebridge.enrollplus.feature.schools.data.local.SchoolEntity
import com.littlebridge.enrollplus.feature.teacher.data.local.TeacherDayCacheDao
import com.littlebridge.enrollplus.feature.teacher.data.local.TeacherDayCacheEntity

@Database(entities = [SchoolEntity::class, OutboxOperationEntity::class, AnnouncementEntity::class, TeacherDayCacheEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun outboxDao(): OutboxDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun teacherDayCacheDao(): TeacherDayCacheDao
    companion object
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        val stmt = connection.prepare(
            """
            CREATE TABLE IF NOT EXISTS outbox_operation (
                id TEXT NOT NULL PRIMARY KEY,
                idempotencyKey TEXT NOT NULL,
                type TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                status TEXT NOT NULL,
                attempts INTEGER NOT NULL,
                nextAttemptAt INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                lastError TEXT
            )
            """.trimIndent()
        )
        stmt.step()
        stmt.close()
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        val stmt = connection.prepare(
            """
            CREATE TABLE IF NOT EXISTS announcement_entity (
                eventId TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                subTitle TEXT,
                description TEXT NOT NULL,
                eventImage TEXT,
                date TEXT NOT NULL,
                audienceType TEXT NOT NULL,
                audienceFilterJson TEXT
            )
            """.trimIndent()
        )
        stmt.step()
        stmt.close()
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        val stmt = connection.prepare(
            """
            CREATE TABLE IF NOT EXISTS teacher_day_cache (
                date TEXT NOT NULL PRIMARY KEY,
                weekday INTEGER NOT NULL,
                isHoliday INTEGER NOT NULL,
                holidayName TEXT,
                periodsJson TEXT NOT NULL,
                calendarJson TEXT NOT NULL,
                nowIndex INTEGER,
                nextIndex INTEGER,
                cachedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        stmt.step()
        stmt.close()
    }
}

// Room generator will provide this on iOS
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect fun AppDatabase.Companion.instantiateImpl(): AppDatabase
