package com.littlebridge.enrollplus.feature.admin.data.local

import com.littlebridge.enrollplus.feature.admin.domain.model.AnnouncementDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryAnnouncementLocalDataSource : AnnouncementLocalDataSource {

    private val cache = MutableStateFlow<List<AnnouncementDto>>(emptyList())

    override fun observeAll(): Flow<List<AnnouncementDto>> = cache.asStateFlow()

    override suspend fun getAll(): List<AnnouncementDto> = cache.value

    override suspend fun saveAll(items: List<AnnouncementDto>) {
        cache.value = items
    }

    override suspend fun deleteAll() {
        cache.value = emptyList()
    }
}
