package com.littlebridge.vidyaprayag.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.littlebridge.vidyaprayag.presentation.landing.CommonLandingScreen
import kotlinx.serialization.Serializable

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
    data object BasicOnboarding : Destination
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

        composable<Destination.BasicOnboarding> {
            com.littlebridge.vidyaprayag.ui.screens.admin.BasicOnboardingScreen()
        }
    }
}
