package com.littlebridge.vidyaprayag.di

import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.schools.data.remote.KtorSchoolApi
import com.littlebridge.vidyaprayag.feature.schools.data.repository.SchoolRepositoryImpl
import com.littlebridge.vidyaprayag.feature.schools.domain.repository.SchoolRepository
import com.littlebridge.vidyaprayag.feature.schools.domain.usecase.GetSchoolsUseCase
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.InstitutionalBasicOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.BrandingInfoOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.AcademicInfoOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.LaunchInfoOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.InstitutionalProfileViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.AdmissionCRMViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolAnnouncementsViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.MessagesViewModel
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
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 15000
            }
        }
    }
    single { KtorSchoolApi(get(), AppConfig.schoolBaseUrl) }
    single { 
        com.littlebridge.vidyaprayag.feature.auth.data.remote.AuthApi(
            client = get(),
            baseUrl = AppConfig.authBaseUrl
        ) 
    }

    // Repositories
    single<SchoolRepository> { SchoolRepositoryImpl(get(), get()) }
    single<com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository> { 
        com.littlebridge.vidyaprayag.feature.auth.data.repository.AuthRepositoryImpl(get()) 
    }

    // UseCases
    factory { GetSchoolsUseCase(get()) }
}

val viewModelModule = module {
    factory { MainViewModel(get(), get()) }
    factory { ParentDashboardViewModel(get()) }
    factory { SchoolDashboardViewModel() }
    factory { InstitutionalBasicOBViewModel() }
    factory { BrandingInfoOBViewModel() }
    factory { AcademicInfoOBViewModel() }
    factory { LaunchInfoOBViewModel() }
    factory { InstitutionalProfileViewModel() }
    factory { AdmissionCRMViewModel() }
    factory { SchoolAnnouncementsViewModel() }
    factory { MessagesViewModel() }
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
