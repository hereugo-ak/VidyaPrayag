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
import io.ktor.client.request.*
import io.ktor.http.*
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
    single {
        com.littlebridge.vidyaprayag.feature.parent.data.remote.ParentApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.OnboardingApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.AdmissionApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.MessagesApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.AnnouncementsApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.PtmApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.CalendarApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.AttendanceApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.LeaveRequestsApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.AnalyticsApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.ResultsApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.UserProfileApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.MediaApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    // Teacher vertical (master doc G1)
    single {
        com.littlebridge.vidyaprayag.feature.teacher.data.remote.TeacherApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }

    // Repositories
    single<SchoolRepository> { SchoolRepositoryImpl(get(), get()) }
    single<com.littlebridge.vidyaprayag.feature.content.domain.repository.ContentRepository> { 
        com.littlebridge.vidyaprayag.feature.content.data.repository.ContentRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository> { 
        com.littlebridge.vidyaprayag.feature.auth.data.repository.AuthRepositoryImpl(get(), get())
    }
    single<com.littlebridge.vidyaprayag.feature.parent.domain.repository.ParentRepository> {
        com.littlebridge.vidyaprayag.feature.parent.data.repository.ParentRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.OnboardingRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.OnboardingRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.AdmissionRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.AdmissionRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.MessagesRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.MessagesRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.AnnouncementsRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.AnnouncementsRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.PtmRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.PtmRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.CalendarRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.CalendarRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.AttendanceRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.AttendanceRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.LeaveRequestsRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.LeaveRequestsRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.AnalyticsRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.AnalyticsRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.ResultsRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.ResultsRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.UserProfileRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.UserProfileRepositoryImpl(get())
    }
    single<com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository> {
        com.littlebridge.vidyaprayag.feature.teacher.data.repository.TeacherRepositoryImpl(get())
    }

    // UseCases
    factory { GetSchoolsUseCase(get()) }
}

val viewModelModule = module {
    factory { MainViewModel(get(), get(), get()) }
    // Schools-discovery marketplace VM — drives DiscoveryScreenV2 off the real
    // GET /api/v1/parent/schools/discover endpoint. See BACKEND_GAPS.md §3.
    factory {
        com.littlebridge.vidyaprayag.feature.schools.presentation.SchoolDiscoveryViewModel(
            get(),  // KtorSchoolApi
            get(),  // PreferenceRepository
        )
    }
    factory { ParentDashboardViewModel(get(), get(), get()) }
    factory { FeeViewModel(get(), get()) }
    factory { ChildBasicInfoViewModel() }
    factory { YourPreferencesViewModel() }
    factory { LocationRequestViewModel() }
    factory { CareerPathViewModel() }
    factory { ScholarshipsViewModel(get(), get()) }
    factory { DailyStatusViewModel() }
    factory { ParentReportsViewModel() }
    factory { ParentSchedulePTMViewModel() }
    factory { ParentAnnouncementViewModel(get(), get()) }
    factory { ParentMessageViewModel() }
    factory { TrackProgressViewModel(get(), get()) }
    factory { SchoolDashboardViewModel(get(), get()) }
    factory { InstitutionalBasicOBViewModel(get(), get()) }
    factory { BrandingInfoOBViewModel(get(), get(), get()) }
    factory { AcademicInfoOBViewModel(get(), get()) }
    factory { LaunchInfoOBViewModel(get(), get()) }
    factory { InstitutionalProfileViewModel(get(), get(), get()) }
    factory { AdmissionCRMViewModel(get(), get()) }
    factory { SchoolAnnouncementsViewModel(get(), get()) }
    factory { MessagesViewModel(get(), get()) }
    factory { SchedulePTMViewModel(get(), get()) }
    factory { AcademicCalendarViewModel(get(), get()) }
    factory { LeaveRequestsViewModel(get(), get()) }
    factory { DailyAttendanceViewModel(get(), get()) }
    factory { AnalyticsDashboardViewModel(get(), get()) }
    factory { StudentAnalyticsViewModel(get(), get()) }
    factory { TeacherPerformanceViewModel(get(), get()) }
    factory { ClassPerformanceViewModel(get(), get()) }
    factory { SyllabusCoverageViewModel(get(), get()) }
    factory { ResultsViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.content.presentation.LandingViewModel(get()) }
    factory { com.littlebridge.vidyaprayag.feature.auth.presentation.AuthViewModel(get()) }
    // Teacher vertical (master doc G1) — 7 VMs, all (TeacherRepository, PreferenceRepository)
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherHomeViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherAttendanceViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherMarksViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherSyllabusViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherHomeworkViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherProfileViewModel(get(), get()) }
}

fun initKoin(
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(commonModule, viewModelModule, platformModule())
}

// For iOS
fun initKoin() = initKoin {}
