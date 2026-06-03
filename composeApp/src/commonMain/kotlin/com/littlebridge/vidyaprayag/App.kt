package com.littlebridge.vidyaprayag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.util.Config
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.NavGraph
import com.littlebridge.vidyaprayag.navigation.ProvideAppNavigator
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.ui.screens.SplashScreen
import com.littlebridge.vidyaprayag.ui.theme.AppTheme
import com.littlebridge.vidyaprayag.ui.theme.VidyaPrayagTheme
import io.ktor.client.*
import okio.FileSystem
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

import org.koin.compose.viewmodel.koinViewModel

@OptIn(KoinExperimentalAPI::class, coil3.annotation.ExperimentalCoilApi::class)
@Composable
@Preview
fun App() {
    KoinContext {
        val viewModel: MainViewModel = koinViewModel()
        val themeName by viewModel.themeName.collectAsState()
        val authState by viewModel.authState.collectAsState()
        
        val httpClient = koinInject<HttpClient>()
        val platform = koinInject<Platform>()

        setSingletonImageLoaderFactory { context: PlatformContext ->
            ImageLoader.Builder(context)
                .components {
                    add(KtorNetworkFetcherFactory(httpClient))
                    // Mapper to strip Supabase tokens from cache keys to avoid re-downloads
                    add(coil3.map.Mapper<io.ktor.http.Url, String> { data, _ ->
                        if (data.host.contains("supabase.co")) {
                            data.toString().substringBefore("?token=")
                        } else {
                            null
                        }
                    })
                }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(context, 0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(platform.cacheDir / "image_cache")
                        .maxSizeBytes(512L * 1024 * 1024) // 512MB
                        .build()
                }
                .logger(coil3.util.DebugLogger())
                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                .crossfade(true)
                .build()
        }

        val appTheme = try { AppTheme.valueOf(themeName) } catch(e: Exception) { AppTheme.LIGHT }

        VidyaPrayagTheme(
            initialTheme = appTheme,
            onThemeChange = { viewModel.setTheme(it.name) }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (!authState.isLoaded) {
                    SplashScreen()
                } else {
                    key(authState.token, authState.role) {
                        val navController = rememberNavController()
                        val startDestination = when {
                            authState.token.isNullOrBlank() -> Destination.Landing
                            authState.role == "ADMIN" -> Destination.SchoolDashboard
                            authState.role == "PARENT" -> Destination.ParentDashboard
                            else -> Destination.Landing
                        }

                        ProvideAppNavigator(navController) {
                            NavGraph(
                                navController = navController,
                                startDestination = startDestination
                            )
                        }
                    }
                }

                // Debug-only backend banner (SCHOOL_SIDE_STATUS_REPORT §8.4).
                // Shows the exact base URL the dev app is using so a phone
                // screenshot proves laptop-vs-Render at a glance. Red = still
                // pointing at Render (devBaseUrl not set), green = laptop/LAN.
                if (Config.isDev) {
                    val isRender = Config.schoolBaseUrl.contains("onrender.com")
                    Text(
                        text = "DEV → ${Config.schoolBaseUrl}" +
                            if (isRender) "  ⚠ set devBaseUrl" else "",
                        color = Color.White,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(if (isRender) Color(0xFFD32F2F) else Color(0xFF2E7D32))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
