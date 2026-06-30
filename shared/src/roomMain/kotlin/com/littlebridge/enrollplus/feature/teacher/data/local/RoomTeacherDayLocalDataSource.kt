package com.littlebridge.enrollplus.feature.teacher.data.local

import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedDayDto

class RoomTeacherDayLocalDataSource(
    private val dao: TeacherDayCacheDao,
) : TeacherDayLocalDataSource {

    override suspend fun getByDate(date: String): ResolvedDayDto? =
        dao.getByDate(date)?.toDomain()

    override suspend fun save(day: ResolvedDayDto) {
        dao.upsert(day.toEntity(cachedAt = currentTimeMillis()))
        if (dao.count() > MAX_DAY_CACHE) {
            dao.evictOldest(MAX_DAY_CACHE)
        }
    }

    override suspend fun evictOlder(keepDate: String) {
        dao.evictOlder(keepDate)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    companion object {
        private const val MAX_DAY_CACHE = 14
    }
}

internal expect fun currentTimeMillis(): Long
