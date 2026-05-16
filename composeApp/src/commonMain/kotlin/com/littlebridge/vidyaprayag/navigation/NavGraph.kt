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

    @Serializable
    data object SchedulePTM : Destination

    @Serializable
    data object AcademicCalendar : Destination

    @Serializable
    data object DailyAttendance : Destination

    @Serializable
    data object LeaveRequests : Destination

    @Serializable
    data object Results : Destination

    @Serializable
    data object AnalyticsDashboard : Destination

    @Serializable
    data object StudentAnalytics : Destination

    @Serializable
    data object ClassPerformance : Destination

    @Serializable
    data object TeacherPerformance : Destination

    @Serializable
    data object SyllabusCoverage : Destination
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
            com.littlebridge.vidyaprayag.ui.screens.parent.ParentDashboardScreen()
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

        composable<Destination.SchedulePTM> {
            com.littlebridge.vidyaprayag.ui.screens.admin.SchedulePTMScreen()
        }

        composable<Destination.AcademicCalendar> {
            com.littlebridge.vidyaprayag.ui.screens.admin.AcademicCalendarScreen()
        }

        composable<Destination.DailyAttendance> {
            com.littlebridge.vidyaprayag.ui.screens.admin.DailyAttendanceScreen()
        }

        composable<Destination.LeaveRequests> {
            com.littlebridge.vidyaprayag.ui.screens.admin.LeaveRequestsScreen()
        }

        composable<Destination.Results> {
            com.littlebridge.vidyaprayag.ui.screens.admin.ResultsScreen()
        }

        composable<Destination.AnalyticsDashboard> {
            com.littlebridge.vidyaprayag.ui.screens.admin.AnalyticsDashboardScreen()
        }

        composable<Destination.StudentAnalytics> {
            com.littlebridge.vidyaprayag.ui.screens.admin.StudentAnalyticsScreen()
        }

        composable<Destination.TeacherPerformance> {
            com.littlebridge.vidyaprayag.ui.screens.admin.TeacherPerformanceScreen()
        }

        composable<Destination.ClassPerformance> {
            com.littlebridge.vidyaprayag.ui.screens.admin.ClassPerformanceScreen()
        }

        composable<Destination.SyllabusCoverage> {
            com.littlebridge.vidyaprayag.ui.screens.admin.SyllabusCoverageScreen()
        }
    }
}
