package com.littlebridge.vidyaprayag.di

import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.schools.data.remote.KtorSchoolApi
import com.littlebridge.vidyaprayag.feature.schools.data.repository.SchoolRepositoryImpl
import com.littlebridge.vidyaprayag.feature.schools.domain.repository.SchoolRepository
import com.littlebridge.vidyaprayag.feature.schools.domain.usecase.GetSchoolsUseCase
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.FeeViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ScholarshipsViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAnnouncementViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.NotificationsViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.LinkChildViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentHomeViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.ParentProfileViewModel
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
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    // Remote
    single {
        val prefs: PreferenceRepository = get()
        // Plain client (NO Auth plugin) used solely to perform the refresh-token
        // exchange, so the bearer refresh path never recurses through itself.
        val refreshClient = HttpClient(get()) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

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

            // Auth: attach the stored access token to every request and, on a
            // 401, automatically exchange the persisted refresh token for a new
            // access token (audit §3.4, finding F). Previously the client had
            // no Auth/401 handling so an expired token surfaced as a generic
            // error and the server's /refresh route was dead code.
            install(Auth) {
                bearer {
                    loadTokens {
                        val access = prefs.getUserToken().first()
                        val refresh = prefs.getRefreshToken().first()
                        if (access != null) BearerTokens(access, refresh ?: "") else null
                    }
                    refreshTokens {
                        val refresh = prefs.getRefreshToken().first()
                            ?: return@refreshTokens null
                        val resp = runCatching {
                            refreshClient.post(AppConfig.authBaseUrl.trimEnd('/') + "/api/v1/auth/refresh") {
                                contentType(ContentType.Application.Json)
                                setBody(mapOf("refresh_token" to refresh))
                            }
                        }.getOrNull() ?: return@refreshTokens null
                        if (!resp.status.isSuccess()) return@refreshTokens null
                        // Parse { success, message, data: { token, refresh_token, ... } }
                        val bodyText = runCatching { resp.bodyAsText() }.getOrNull()
                            ?: return@refreshTokens null
                        val data = runCatching {
                            Json { ignoreUnknownKeys = true }
                                .parseToJsonElement(bodyText)
                                .jsonObject["data"]?.jsonObject
                        }.getOrNull() ?: return@refreshTokens null
                        val newAccess = data["token"]?.jsonPrimitive?.contentOrNull
                            ?: return@refreshTokens null
                        val newRefresh = data["refresh_token"]?.jsonPrimitive?.contentOrNull
                            ?: refresh
                        prefs.setUserToken(newAccess)
                        prefs.setRefreshToken(newRefresh)
                        BearerTokens(newAccess, newRefresh)
                    }
                }
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
        com.littlebridge.vidyaprayag.feature.admin.data.remote.TeachersApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    // RA-47: institutional-profile (schools row) read/edit
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.SchoolProfileApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    // RA-45: student roster + student/teacher profile detail
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.StudentsApi(
            client = get(),
            baseUrl = AppConfig.schoolBaseUrl
        )
    }
    // RA-52: admin Records rollups (attendance / marks / fees)
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.RecordsApi(
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
    // RA-48: school-admin link-request queue API.
    single {
        com.littlebridge.vidyaprayag.feature.admin.data.remote.LinkRequestsApi(
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
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.TeachersRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.TeachersRepositoryImpl(get())
    }
    // RA-47
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.SchoolProfileRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.SchoolProfileRepositoryImpl(get())
    }
    // RA-45
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.StudentsRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.StudentsRepositoryImpl(get())
    }
    // RA-52
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.RecordsRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.RecordsRepositoryImpl(get())
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
    // RA-48: school-admin link-request queue repository.
    single<com.littlebridge.vidyaprayag.feature.admin.domain.repository.LinkRequestsRepository> {
        com.littlebridge.vidyaprayag.feature.admin.data.repository.LinkRequestsRepositoryImpl(get())
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
    // NOTE (SWEEP-B): the following V1 parent VMs were registered here but had
    // ZERO injection/usage anywhere after the V1->V2 screen migration
    // (ParentDashboardViewModel superseded by ParentHomeViewModel + SchoolDiscoveryViewModel;
    //  ChildBasicInfo/YourPreferences/LocationRequest/CareerPath/DailyStatus/ParentReports/
    //  ParentSchedulePTM/ParentMessage screens dropped in V2). Their dead factory
    // registrations were removed to keep the Koin graph clean — every remaining
    // registration below is actually consumed via koinViewModel().
    factory { FeeViewModel(get(), get()) }
    factory { ScholarshipsViewModel(get(), get()) }
    factory { ParentAnnouncementViewModel(get(), get()) }
    factory { NotificationsViewModel(get(), get()) }
    factory { LinkChildViewModel(get(), get()) }
    factory { ParentHomeViewModel(get(), get()) }
    factory { ParentProfileViewModel(get(), get()) }
    factory { TrackProgressViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.parent.presentation.ParentAcademicsViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.parent.presentation.ParentLeaveViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.parent.presentation.ParentMessageViewModel(get(), get()) }
    factory { SchoolDashboardViewModel(get(), get()) }
    factory { InstitutionalBasicOBViewModel(get(), get()) }
    factory { BrandingInfoOBViewModel(get(), get(), get()) }
    factory { AcademicInfoOBViewModel(get(), get()) }
    factory { LaunchInfoOBViewModel(get(), get()) }
    factory { InstitutionalProfileViewModel(get(), get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolProfileViewModel(get(), get()) } // RA-47
    factory { com.littlebridge.vidyaprayag.feature.admin.presentation.StudentRosterViewModel(get(), get()) } // RA-45
    factory { com.littlebridge.vidyaprayag.feature.admin.presentation.StudentProfileViewModel(get(), get()) } // RA-45
    factory { com.littlebridge.vidyaprayag.feature.admin.presentation.TeacherProfileViewModel(get(), get()) } // RA-45
    factory { com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolRecordsViewModel(get(), get()) } // RA-52
    factory { AdmissionCRMViewModel(get(), get()) }
    factory { SchoolAnnouncementsViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolTeachersViewModel(get(), get()) }
    factory { MessagesViewModel(get(), get()) }
    factory { SchedulePTMViewModel(get(), get()) }
    factory { AcademicCalendarViewModel(get(), get()) }
    factory { LeaveRequestsViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.admin.presentation.LinkRequestsViewModel(get(), get()) }
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
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherAssessmentsViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherSyllabusViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherHomeworkViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherProfileViewModel(get(), get()) }
    factory { com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherLeaveViewModel(get(), get()) }
}

fun initKoin(
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(commonModule, viewModelModule, platformModule())
}

// For iOS
fun initKoin() = initKoin {}
