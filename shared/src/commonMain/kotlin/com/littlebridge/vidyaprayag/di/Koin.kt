package com.littlebridge.vidyaprayag.di

import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.schools.data.remote.KtorSchoolApi
import com.littlebridge.vidyaprayag.feature.schools.data.repository.SchoolRepositoryImpl
import com.littlebridge.vidyaprayag.feature.schools.domain.repository.SchoolRepository
import com.littlebridge.vidyaprayag.feature.schools.domain.usecase.GetSchoolsUseCase
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.FeeViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ChildBasicInfoViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.YourPreferencesViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.LocationRequestViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.CareerPathViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ScholarshipsViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.DailyStatusViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentReportsViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentSchedulePTMViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAnnouncementViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentMessageViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.InstitutionalBasicOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.BrandingInfoOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.AcademicInfoOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.LaunchInfoOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.InstitutionalProfileViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.AdmissionCRMViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolAnnouncementsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.MessagesViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchedulePTMViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.AcademicCalendarViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.LeaveRequestsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.DailyAttendanceViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.AnalyticsDashboardViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.StudentAnalyticsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.TeacherPerformanceViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.ClassPerformanceViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SyllabusCoverageViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.ResultsViewModel
import com.littlebridge.vidyaprayag.util.AppConfig
import com.littlebridge.vidyaprayag.util.AppLogger
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    // Remote
    single {
        HttpClient(get()) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            
            install(HttpRedirect) {
                checkHttpMethod = false
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                connectTimeoutMillis = 60000
                socketTimeoutMillis = 60000
            }
        }
    }
    single { KtorSchoolApi(get(), AppConfig.schoolBaseUrl) }
    single { 
        com.littlebridge.vidyaprayag.feature.content.data.remote.ContentApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single { 
        com.littlebridge.vidyaprayag.feature.auth.data.remote.AuthApi(
            client = get(),
            baseUrl = AppConfig.authBaseUrl
        ) 
    }

    // Repositories
    single<SchoolRepository> { SchoolRepositoryImpl(get(), get()) }
    single<com.littlebridge.vidyaprayag.feature.content.domain.repository.ContentRepository> { 
        com.littlebridge.vidyaprayag.feature.content.data.repository.ContentRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository> { 
        com.littlebridge.vidyaprayag.feature.auth.data.repository.AuthRepositoryImpl(get()) 
    }

    // UseCases
    factory { GetSchoolsUseCase(get()) }
}

val viewModelModule = module {
    factory { MainViewModel(get(), get()) }
    factory { ParentDashboardViewModel(get()) }
    factory { FeeViewModel() }
    factory { ChildBasicInfoViewModel() }
    factory { YourPreferencesViewModel() }
    factory { LocationRequestViewModel() }
    factory { CareerPathViewModel() }
    factory { ScholarshipsViewModel() }
    factory { DailyStatusViewModel() }
    factory { ParentReportsViewModel() }
    factory { ParentSchedulePTMViewModel() }
    factory { ParentAnnouncementViewModel() }
    factory { ParentMessageViewModel() }
    factory { TrackProgressViewModel() }
    factory { SchoolDashboardViewModel() }
    factory { InstitutionalBasicOBViewModel() }
    factory { BrandingInfoOBViewModel() }
    factory { AcademicInfoOBViewModel() }
    factory { LaunchInfoOBViewModel() }
    factory { InstitutionalProfileViewModel() }
    factory { AdmissionCRMViewModel() }
    factory { SchoolAnnouncementsViewModel() }
    factory { MessagesViewModel() }
    factory { SchedulePTMViewModel() }
    factory { AcademicCalendarViewModel() }
    factory { LeaveRequestsViewModel() }
    factory { DailyAttendanceViewModel() }
    factory { AnalyticsDashboardViewModel() }
    factory { StudentAnalyticsViewModel() }
    factory { TeacherPerformanceViewModel() }
    factory { ClassPerformanceViewModel() }
    factory { SyllabusCoverageViewModel() }
    factory { ResultsViewModel() }
    factory { com.littlebridge.vidyaprayag.feature.content.presentation.LandingViewModel(get()) }
    factory { com.littlebridge.vidyaprayag.feature.auth.presentation.AuthViewModel(get()) }
}

fun initKoin(
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(commonModule, viewModelModule, platformModule())
}

// For iOS
fun initKoin() = initKoin {}
