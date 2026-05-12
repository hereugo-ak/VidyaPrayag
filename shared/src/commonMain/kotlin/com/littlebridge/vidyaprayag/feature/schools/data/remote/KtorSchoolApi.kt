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
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCtZsoBnpomGaN5EmO_sG73GoM7J9ZLKKnPf-RmDqu2iRw3Owx3jjCXoj-AJOTPxM7R0RtbCNz6bX6uhy1m8y7TIUM6FZSYYGtbJ7itQgKPY48-QLsdU7_Rpc3iOrNCMcemyAMN8KlVQeAXGOIK-ZBmJLHBR6lvxVz2WzC3kkpPJNmiAds7YP4S-NLoy9KlTIFnxMOX1_JjwqNB9Xk-kPo0aLXznyaE5qUO-KBA0UspY9L02mfUrkr_3WN1t-8dsYPdGnS5mPLE2H9g",
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
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBCDgaG5pYAxIrxIM71VX7W_KV-XCGUOA4piv3W2rOdG8Bjdm2_gM83qiJAQlXg4LSckIzlG-l007_XJYutSP9o2Q1zgoEIatN1W_Ij9RFw2fVFKyGQoQeYc67rH9Gm_8UqxUMds-MKurCL_fwgrq4D-KGqinBzv9Gndd82J1da4EFudHnB57-3RmlNjUCfnB7vnl6Ro2bbRkBJW2-RjHc12QqVrA0rQX6jcp3uyVob_pxNK-yKUu40IFBoSxy3TagSkjoL420fvEPf",
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
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBtpb6PQ46MMzzY51ow29NBhn4tMu8O3VK2an3c7mZERdhEWZFHMd03hv7uWKNUmVzoJE1EUX3yiwdndALTXmEd63AJgsHNpih7Bb1ULia_8O5VppmFDifXwKmGxIThSOAJKAxP-q1dfafFV85BsotUbO2LtYDCZfBi4LLS-lfS1SB6LWrf32mYmr98CiBIABuXGa1ZwT-X29YmHKxHHzJW-ICuHjgESZedb7tOX96EI4NXj36dC0i7IVNJ8617m_Yvk8d9rSrAkqi5",
                sriScore = 9.7,
                feesRange = "$32k - $40k",
                isVerified = true
            )
        )
    }
}
