package com.littlebridge.enrollplus.feature.teacher.data.local

import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedDayDto

class InMemoryTeacherDayLocalDataSource : TeacherDayLocalDataSource {

    private var cache: ResolvedDayDto? = null

    override suspend fun getByDate(date: String): ResolvedDayDto? =
        cache?.takeIf { it.date == date }

    override suspend fun save(day: ResolvedDayDto) {
        cache = day
    }

    override suspend fun evictOlder(keepDate: String) {
        if (cache?.date != keepDate) cache = null
    }

    override suspend fun deleteAll() {
        cache = null
    }
}
