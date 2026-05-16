package com.littlebridge.vidyaprayag

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.littlebridge.vidyaprayag.navigation.NavGraph
import com.littlebridge.vidyaprayag.navigation.ProvideAppNavigator
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.ui.theme.AppTheme
import com.littlebridge.vidyaprayag.ui.theme.VidyaPrayagTheme
import org.koin.compose.KoinContext
import org.koin.core.annotation.KoinExperimentalAPI

import org.koin.compose.viewmodel.koinViewModel

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    KoinContext {
        val viewModel: MainViewModel = koinViewModel()
        val themeName by viewModel.themeName.collectAsState()
        
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
