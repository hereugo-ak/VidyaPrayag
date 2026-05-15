package com.littlebridge.vidyaprayag.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.littlebridge.vidyaprayag.presentation.landing.CommonLandingScreen
import kotlinx.serialization.Serializable
import androidx.compose.material3.Text

@Serializable
sealed interface Destination {
    @Serializable
    data object Landing : Destination
    
    @Serializable
    data class SchoolDetails(val id: String) : Destination
    
    @Serializable
    data object Search : Destination

    @Serializable
    data object ParentDashboard : Destination

    @Serializable
    data object SchoolDashboard : Destination

    @Serializable
    data object InstitutionalBasicOB : Destination
    
    @Serializable
    data object BrandingInfoOB : Destination
    
    @Serializable
    data object AcademicInfoOB : Destination

    @Serializable
    data object LaunchInfoOB : Destination

    @Serializable
    data object InstitutionalProfile : Destination

    @Serializable
    data object AdmissionCRMDashboard : Destination

    @Serializable
    data object SchoolAnnouncements : Destination

    @Serializable
    data object Messages : Destination
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destination.Landing
    ) {
        composable<Destination.Landing> {
            CommonLandingScreen()
        }
        
        composable<Destination.Search> {
            // Placeholder for Search Screen
        }
        
        composable<Destination.SchoolDetails> {
            // Placeholder for Details Screen
        }

        composable<Destination.ParentDashboard> {
            com.littlebridge.vidyaprayag.ui.screens.dashboard.ParentDashboardScreen()
        }

        composable<Destination.SchoolDashboard> {
            com.littlebridge.vidyaprayag.ui.screens.admin.SchoolDashboardScreen()
        }

        composable<Destination.InstitutionalBasicOB> {
            com.littlebridge.vidyaprayag.ui.screens.admin.InstitutionalBasicOBScreen()
        }

        composable<Destination.BrandingInfoOB> {
            com.littlebridge.vidyaprayag.ui.screens.admin.BrandingInfoOBScreen()
        }

        composable<Destination.AcademicInfoOB> {
            com.littlebridge.vidyaprayag.ui.screens.admin.AcademicInfoOBScreen()
        }

        composable<Destination.LaunchInfoOB> {
            com.littlebridge.vidyaprayag.ui.screens.admin.LaunchInfoOBScreen()
        }

        composable<Destination.InstitutionalProfile> {
            com.littlebridge.vidyaprayag.ui.screens.admin.InstitutionalProfileScreen()
        }

        composable<Destination.AdmissionCRMDashboard> {
            com.littlebridge.vidyaprayag.ui.screens.admin.AdmissionCRMDashboard()
        }

        composable<Destination.SchoolAnnouncements> {
            com.littlebridge.vidyaprayag.ui.screens.admin.SchoolAnnouncementsScreen()
        }

        composable<Destination.Messages> {
            com.littlebridge.vidyaprayag.ui.screens.admin.MessagesScreen()
        }
    }
}
