package com.littlebridge.vidyaprayag.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

interface AppNavigator {
    fun navigateTo(destination: Destination)
    fun navigateToAndClear(destination: Destination)
    fun goBack()
}

class ComposeAppNavigator(
    private val navController: NavHostController
) : AppNavigator {
    override fun navigateTo(destination: Destination) {
        navController.navigate(destination)
    }

    override fun navigateToAndClear(destination: Destination) {
        navController.navigate(destination) {
            popUpTo(0) { inclusive = true }
        }
    }

    override fun goBack() {
        navController.popBackStack()
    }
}

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> {
    error("No AppNavigator provided")
}

@Composable
fun ProvideAppNavigator(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val navigator = ComposeAppNavigator(navController)
    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        content()
    }
}
