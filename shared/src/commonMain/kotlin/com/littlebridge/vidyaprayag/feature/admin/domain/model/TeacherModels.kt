/*
 * File: TeacherModels.kt
 * Module: feature.admin.domain.model
 *
 * DTOs for the school teacher-provisioning API (RA-22).
 * Matches server: feature.school.TeacherProvisioningRouting.kt
 *   GET    /api/v1/school/teachers
 *   POST   /api/v1/school/teachers
 *   DELETE /api/v1/school/teachers/{id}
 *   POST   /api/v1/school/teachers/{id}/reset-password  (RA-32)
 */
package com.littlebridge.vidyaprayag.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeacherAccountDto(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val role: String,
    @SerialName("school_id") val schoolId: String
)

@Serializable
data class TeacherListResponse(
    val teachers: List<TeacherAccountDto>
)

@Serializable
data class CreateTeacherRequest(
    val name: String,
    val identifier: String,                                  // email OR phone
    @SerialName("initial_password") val initialPassword: String? = null
)

/** RA-32: server returns the freshly-issued plaintext password exactly once. */
@Serializable
data class TeacherCredentialDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("initial_password") val initialPassword: String
)
