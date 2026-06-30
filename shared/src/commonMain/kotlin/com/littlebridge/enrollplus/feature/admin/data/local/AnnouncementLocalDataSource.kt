package com.littlebridge.enrollplus.feature.admin.data.local

import com.littlebridge.enrollplus.feature.admin.domain.model.AnnouncementDto
import kotlinx.coroutines.flow.Flow

interface AnnouncementLocalDataSource {
    fun observeAll(): Flow<List<AnnouncementDto>>
    suspend fun getAll(): List<AnnouncementDto>
    suspend fun saveAll(items: List<AnnouncementDto>)
    suspend fun deleteAll()
}
