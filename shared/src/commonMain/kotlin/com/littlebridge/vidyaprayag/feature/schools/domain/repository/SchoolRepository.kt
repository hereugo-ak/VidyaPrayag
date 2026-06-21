package com.littlebridge.vidyaprayag.feature.schools.domain.repository

import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import kotlinx.coroutines.flow.Flow

interface SchoolRepository {
    fun getSchools(): Flow<List<School>>
    suspend fun refreshSchools()
    suspend fun getSchoolById(id: String): School?
    /** Clears all locally cached school data (e.g. on logout). */
    suspend fun clearCache()
}
