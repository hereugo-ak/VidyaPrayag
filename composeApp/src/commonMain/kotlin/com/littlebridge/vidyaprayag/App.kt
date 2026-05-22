package com.littlebridge.vidyaprayag

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.littlebridge.vidyaprayag.navigation.NavGraph
import com.littlebridge.vidyaprayag.navigation.ProvideAppNavigator
import com.littlebridge.vidyaprayag.presentation.MainViewModel
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
                        .fileSystem(okio.FileSystem.SYSTEM)
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
            val navController = rememberNavController()
            ProvideAppNavigator(navController) {
                NavGraph(navController = navController)
            }
        }
    }
}
