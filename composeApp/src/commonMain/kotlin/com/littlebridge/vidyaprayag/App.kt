package com.littlebridge.vidyaprayag

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.ui.v2.navigation.NavGraphV2
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.SplashScreenV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VColors
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.vColorsFor
import com.littlebridge.vidyaprayag.util.Config
import io.ktor.client.*
import okio.FileSystem
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

/**
 * Application entrypoint — drives the **`ui/v2`** design-system UI exclusively.
 *
 * The legacy `ui/` (theme/components/auth/screens) and the old 35-destination
 * `navigation/NavGraph.kt` have been removed; navigation is now role-driven through
 * [NavGraphV2], which selects the correct portal (`SchoolPortalV2` / `TeacherPortalV2` /
 * `ParentPortalV2`) and applies the matching [VPortalTone].
 *
 * Flow:
 *  - `KoinContext` → [MainViewModel] (auth state).
 *  - Install the Coil image loader (Ktor fetcher + Supabase token-stripping cache mapper).
 *  - While auth is still loading → a minimal lavender splash with a spinner.
 *  - Once loaded → [NavGraphV2] with `role` + `isAuthenticated` + `onLogout`.
 */
@OptIn(KoinExperimentalAPI::class, coil3.annotation.ExperimentalCoilApi::class)
@Composable
@Preview
fun App(
    // FEATURE 1: fired once Compose has drawn its first frame so the Android host
    // can dismiss the native SplashScreen with zero white flash. No-op default keeps
    // iOS / desktop / @Preview callers unchanged (RULE-5: commonMain-safe).
    onContentRendered: () -> Unit = {},
) {
    KoinContext {
        // Signal the platform host after the first composition lands. SideEffect runs
        // after every successful recomposition; the host only reads the first flip.
        SideEffect { onContentRendered() }

        val viewModel: MainViewModel = koinViewModel()
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

        // Resolve the colors for a lavender (Light) splash before the portal tone is known.
        val splashColors: VColors = vColorsFor(VPortalTone.Light)

        Box(modifier = Modifier.fillMaxSize().background(splashColors.background)) {
            // PHASE 2 — Splash shows the brand while the session check (JWT + role) runs in
            // parallel inside MainViewModel.authState. The instant `isLoaded` flips true we
            // crossfade straight into the role-driven graph: no artificial hold, no blank frame.
            AnimatedContent(
                targetState = authState.isLoaded,
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(220)) },
                label = "splash-to-app",
                modifier = Modifier.fillMaxSize(),
            ) { loaded ->
                if (!loaded) {
                    SplashScreenV2(modifier = Modifier.fillMaxSize())
                } else {
                    val isAuthenticated = !authState.token.isNullOrBlank()
                    NavGraphV2(
                        role = authState.role,
                        isAuthenticated = isAuthenticated,
                        onLogout = { viewModel.logout() },
                        modifier = Modifier.fillMaxSize(),
                    )
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
