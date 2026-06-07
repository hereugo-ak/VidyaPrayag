package com.littlebridge.vidyaprayag.feature.parent.data.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.parent.data.remote.ParentApi
import com.littlebridge.vidyaprayag.feature.parent.domain.model.*
import com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository

class ParentRepositoryImpl(
    private val api: ParentApi
) : ParentRepository {
    override suspend fun getTrackProgress(token: String): NetworkResult<TrackProgressResponse> {
        return api.getTrackProgress(token)
    }

    override suspend fun getFees(token: String): NetworkResult<FeeResponse> {
        return api.getFees(token)
    }

    override suspend fun getScholarships(token: String): NetworkResult<ScholarshipsResponse> {
        return api.getScholarships(token)
    }

    override suspend fun getAnnouncements(token: String): NetworkResult<ParentAnnouncementsResponse> {
        return api.getAnnouncements(token)
    }

    override suspend fun getNotifications(token: String): NetworkResult<ParentNotificationsResponse> {
        return api.getNotifications(token)
    }
}
