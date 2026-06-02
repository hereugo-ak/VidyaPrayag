package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.ClassDetailsResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingStepResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingSubmitRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingSubmitResponse

/**
 * Domain-layer abstraction over the school onboarding endpoints.
 *
 * Implementations are responsible for talking to the network layer
 * (typically via [com.littlebridge.vidyaprayag.feature.admin.data.remote.OnboardingApi])
 * and unwrapping the server's ApiResponse envelope so callers
 * (ViewModels) only deal with the inner data types.
 */
interface OnboardingRepository {

    /**
     * Fetches the saved state / form schema for a single onboarding step.
     *
     * @param token JWT bearer token (no "Bearer " prefix; the API adds it)
     * @param obStepType One of: "BASIC", "BRANDING", "ACADEMIC", "REVIEW"
     */
    suspend fun getStep(
        token: String,
        obStepType: String
    ): NetworkResult<OnboardingStepResponse>

    /**
     * Persists a step's data (and optionally marks onboarding as complete).
     */
    suspend fun submitStep(
        token: String,
        request: OnboardingSubmitRequest
    ): NetworkResult<OnboardingSubmitResponse>

    /**
     * Fetches the subject / teacher details for a single class in the ACADEMIC step.
     *
     * @param classId The class code (e.g. "8", "NURSERY") as returned by [getStep] for ACADEMIC.
     */
    suspend fun getClassDetails(
        token: String,
        classId: String
    ): NetworkResult<ClassDetailsResponse>
}
