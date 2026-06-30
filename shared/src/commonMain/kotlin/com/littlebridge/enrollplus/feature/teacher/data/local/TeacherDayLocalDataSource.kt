package com.littlebridge.enrollplus.feature.teacher.data.local

import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedDayDto

interface TeacherDayLocalDataSource {
    suspend fun getByDate(date: String): ResolvedDayDto?
    suspend fun save(day: ResolvedDayDto)
    suspend fun evictOlder(keepDate: String)
    suspend fun deleteAll()
}
