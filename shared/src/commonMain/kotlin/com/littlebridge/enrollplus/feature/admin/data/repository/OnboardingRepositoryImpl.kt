package com.littlebridge.enrollplus.feature.admin.data.repository

import com.littlebridge.enrollplus.core.cache.CacheManager
import com.littlebridge.enrollplus.core.cache.cacheFirstNetworkResult
import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.data.remote.OnboardingApi
import com.littlebridge.enrollplus.feature.admin.domain.model.ClassDetailsResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.OnboardingCompletionResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.OnboardingStatusResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.OnboardingStepResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.OnboardingSubmitRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.OnboardingSubmitResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SetupProgress
import com.littlebridge.enrollplus.feature.admin.domain.repository.OnboardingRepository

/**
 * Default [OnboardingRepository] implementation.
 *
 * Mirrors the pattern used by AuthRepositoryImpl: takes the ApiResponse envelope
 * returned by the server and unwraps `.data`, surfacing a clean error if the
 * server returned `success=false` (or the data field was missing).
 */
class OnboardingRepositoryImpl(
    private val api: OnboardingApi,
    private val cache: CacheManager,
) : OnboardingRepository {

    override suspend fun getStep(
        token: String,
        obStepType: String
    ): NetworkResult<OnboardingStepResponse> {
        val r = cacheFirstNetworkResult(cache, "admin_ob_step_$obStepType", ApiResponse.serializer(OnboardingStepResponse.serializer())) { api.getStep(token, obStepType) }
        return when (r) {
            is NetworkResult.Success -> {
                val envelope = r.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(envelope.message.ifBlank { "Failed to fetch onboarding step" })
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data, isStale = r.isStale, isOffline = r.isOffline)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun submitStep(
        token: String,
        request: OnboardingSubmitRequest
    ): NetworkResult<OnboardingSubmitResponse> {
        return when (val result = api.submitStep(token, request)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(
                        envelope.message.ifBlank { "Failed to submit onboarding step" }
                    )
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getClassDetails(
        token: String,
        classId: String
    ): NetworkResult<ClassDetailsResponse> {
        val r = cacheFirstNetworkResult(cache, "admin_ob_class_details_$classId", ApiResponse.serializer(ClassDetailsResponse.serializer())) { api.getClassDetails(token, classId) }
        return when (r) {
            is NetworkResult.Success -> {
                val envelope = r.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(envelope.message.ifBlank { "Failed to fetch class details" })
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data, isStale = r.isStale, isOffline = r.isOffline)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getStatus(
        token: String
    ): NetworkResult<OnboardingStatusResponse> {
        val r = cacheFirstNetworkResult(cache, "admin_ob_status", ApiResponse.serializer(OnboardingStatusResponse.serializer())) { api.getStatus(token) }
        return when (r) {
            is NetworkResult.Success -> {
                val envelope = r.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(envelope.message.ifBlank { "Failed to fetch onboarding status" })
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data, isStale = r.isStale, isOffline = r.isOffline)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getSetupProgress(token: String): NetworkResult<SetupProgress> {
        return when (val result = api.getSetupProgress(token)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(envelope.message.ifBlank { "Failed to fetch school setup progress" })
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun completeOnboarding(
        token: String
    ): NetworkResult<OnboardingCompletionResponse> {
        return when (val result = api.completeOnboarding(token)) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                when {
                    !envelope.success -> NetworkResult.Error(
                        envelope.message.ifBlank { "Failed to complete onboarding" }
                    )
                    data == null -> NetworkResult.Error("No data in response")
                    else -> NetworkResult.Success(data)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }
}
