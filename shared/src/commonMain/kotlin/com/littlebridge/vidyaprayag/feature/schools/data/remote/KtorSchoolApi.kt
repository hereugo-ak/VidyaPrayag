package com.littlebridge.vidyaprayag.feature.schools.data.remote

import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class KtorSchoolApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    // In a real app, this would be an actual URL.
    // For now, we simulate a network response.
    suspend fun fetchSchools(): List<School> {
        // Mocking behavior for demonstration
        return listOf(
            School(
                id = "1", 
                name = "St. Xavier\'s Global Academy", 
                location = "Mumbai", 
                board = "CBSE", 
                description = "Focus on holistic development with world-class IB curriculum and Olympic-sized sports facilities.", 
                imageUrl = "https://picsum.photos/seed/vidyaprayag-school-1/900/520",
                sriScore = 9.4,
                feesRange = "$24k - $28k",
                isVerified = true
            ),
            School(
                id = "2", 
                name = "Pinnacle International", 
                location = "Delhi", 
                board = "IB", 
                description = "Award-winning STEM lab and arts conservatory programs located in the heart of the tech district.", 
                imageUrl = "https://picsum.photos/seed/vidyaprayag-school-2/900/520",
                sriScore = 8.9,
                feesRange = "$18k - $22k",
                isVerified = true
            ),
            School(
                id = "3", 
                name = "Merit Hall Prep", 
                location = "Bangalore", 
                board = "IGCSE", 
                description = "Ivy-League pathway school with a 100% placement rate in top-tier global universities.", 
                imageUrl = "https://picsum.photos/seed/vidyaprayag-school-3/900/520",
                sriScore = 9.7,
                feesRange = "$32k - $40k",
                isVerified = true
            )
        )
    }
}
