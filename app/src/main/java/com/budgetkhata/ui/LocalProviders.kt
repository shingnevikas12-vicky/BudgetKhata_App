package com.budgetkhata.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.SnackbarHostState
import com.budgetkhata.data.prefs.AppSettings

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }
