package com.littlebridge.enrollplus.feature.admin.data.local

import com.littlebridge.enrollplus.feature.admin.domain.model.AnnouncementDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAnnouncementLocalDataSource(
    private val dao: AnnouncementDao,
) : AnnouncementLocalDataSource {

    override fun observeAll(): Flow<List<AnnouncementDto>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<AnnouncementDto> =
        dao.getAll().map { it.toDomain() }

    override suspend fun saveAll(items: List<AnnouncementDto>) {
        dao.deleteAll()
        dao.insertAll(items.map { it.toEntity() })
        if (dao.count() > MAX_ANNOUNCEMENTS) {
            dao.evictOldest(MAX_ANNOUNCEMENTS)
        }
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    companion object {
        private const val MAX_ANNOUNCEMENTS = 200
    }
}
