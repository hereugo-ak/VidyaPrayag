package com.littlebridge.vidyaprayag.di

import com.littlebridge.vidyaprayag.core.prefs.PreferenceRepository
import com.littlebridge.vidyaprayag.feature.schools.data.remote.KtorSchoolApi
import com.littlebridge.vidyaprayag.feature.schools.data.repository.SchoolRepositoryImpl
import com.littlebridge.vidyaprayag.feature.schools.domain.repository.SchoolRepository
import com.littlebridge.vidyaprayag.feature.schools.domain.usecase.GetSchoolsUseCase
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.presentation.ParentDashboardViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.BasicOnboardingViewModel
import com.littlebridge.vidyaprayag.util.AppConfig
import com.littlebridge.vidyaprayag.util.Environment
import com.littlebridge.vidyaprayag.util.AppLogger
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    // Remote
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            
            // Comprehensive Logging Workaround
            install(ResponseObserver) {
                onResponse { response ->
                    AppLogger.d("HTTP Client", "--- RESPONSE RECEIVED ---")
                    AppLogger.d("HTTP Client", "URL: ${response.call.request.url}")
                    AppLogger.d("HTTP Client", "METHOD: ${response.call.request.method}")
                    AppLogger.d("HTTP Client", "STATUS: ${response.status.value} ${response.status.description}")
                    AppLogger.d("HTTP Client", "HEADERS: ${response.headers}")
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                AppLogger.d("HTTP Client", "--- REQUEST PREPARING ---")
                AppLogger.d("HTTP Client", "URL: ${this.url}")
            }
        }
    }
    single { KtorSchoolApi(get()) }
    single { 
        com.littlebridge.vidyaprayag.feature.auth.data.remote.AuthApi(
            client = get(),
            baseUrl = AppConfig.current.baseUrl
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
    factory { BasicOnboardingViewModel() }
    factory { com.littlebridge.vidyaprayag.feature.auth.presentation.AuthViewModel(get()) }
}

fun initKoin(
    environment: Environment = Environment.DEV,
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    AppConfig.current = environment
    appDeclaration()
    modules(commonModule, viewModelModule, platformModule())
}

// For iOS
fun initKoin() = initKoin {}
