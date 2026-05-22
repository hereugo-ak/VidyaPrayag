package com.littlebridge.vidyaprayag.feature.content.domain.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.content.domain.model.LandingData

interface ContentRepository {
    suspend fun getLandingContent(): NetworkResult<LandingData>
}
