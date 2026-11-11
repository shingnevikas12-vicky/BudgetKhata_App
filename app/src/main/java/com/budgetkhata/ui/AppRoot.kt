package com.budgetkhata.ui

import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetkhata.data.prefs.AppSettings
import com.budgetkhata.data.prefs.ThemeMode
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.navigation.AppNavigation
import com.budgetkhata.ui.theme.BudgetKhataTheme
import java.util.Locale

@Composable
fun BudgetKhataRoot(container: AppContainer) {
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    
    // Apply language
    val locale = remember(settings.language) { Locale(settings.language.code) }
    val resources = context.resources
    val configuration = resources.configuration
    if (configuration.locales[0] != locale) {
        configuration.setLocale(locale)
        context.createConfigurationContext(configuration)
        // Note: For full app translation without restart, we usually need to update the 
        // global resources for the application context as well, or use a custom ContextWrapper.
        // In this simple app, we can update the base resources.
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    LaunchedEffect(settings.screenshotProtection) {
        val window = context.findActivity()?.window ?: return@LaunchedEffect
        if (settings.screenshotProtection) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    BudgetKhataTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
        CompositionLocalProvider(LocalAppSettings provides settings) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavigation(container)
            }
        }
    }
}

data class BottomDestination(
    val base: String,
    val route: String,
    val label: String,
    val icon: ImageVector
)
