package com.budgetkhata.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFF4338CA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF12131A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12131A),
    surfaceVariant = Color(0xFFE6E8F4),
    onSurfaceVariant = Color(0xFF464A5E),
    surfaceContainer = Color(0xFFF1F3FB),
    surfaceContainerHigh = Color(0xFFEAEDF8),
    surfaceContainerHighest = Color(0xFFE3E7F4),
    surfaceContainerLow = Color(0xFFF7F8FE),
    outline = Color(0xFF767B91),
    outlineVariant = Color(0xFFC7CAD9),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inversePrimary = Color(0xFFA5B4FC),
    inverseSurface = Color(0xFF2E3038),
    inverseOnSurface = Color(0xFFF1F0F5),
    scrim = Color(0xFF000000)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF0F1220),
    onBackground = Color(0xFFE6E8F4),
    surface = Color(0xFF161A2B),
    onSurface = Color(0xFFE6E8F4),
    surfaceVariant = Color(0xFF2A3050),
    onSurfaceVariant = Color(0xFFBFC4DA),
    surfaceContainer = Color(0xFF1B2036),
    surfaceContainerHigh = Color(0xFF212742),
    surfaceContainerHighest = Color(0xFF2A3050),
    surfaceContainerLow = Color(0xFF131727),
    outline = Color(0xFF8B90A8),
    outlineVariant = Color(0xFF3B4162),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inversePrimary = Color(0xFF4338CA),
    inverseSurface = Color(0xFFE6E8F4),
    inverseOnSurface = Color(0xFF161A2B),
    scrim = Color(0xFF000000)
)

data class StatusPalette(
    val paid: Color,
    val paidContainer: Color,
    val onPaidContainer: Color,
    val partial: Color,
    val partialContainer: Color,
    val onPartialContainer: Color,
    val pending: Color,
    val pendingContainer: Color,
    val onPendingContainer: Color,
    val income: Color,
    val expense: Color
)

val LightStatusPalette = StatusPalette(
    paid = Color(0xFF15803D),
    paidContainer = Color(0xFFDCFCE7),
    onPaidContainer = Color(0xFF14532D),
    partial = Color(0xFFC2410C),
    partialContainer = Color(0xFFFFEDD5),
    onPartialContainer = Color(0xFF7C2D12),
    pending = Color(0xFFB91C1C),
    pendingContainer = Color(0xFFFEE2E2),
    onPendingContainer = Color(0xFF7F1D1D),
    income = Color(0xFF0F766E),
    expense = Color(0xFFC2410C)
)

val DarkStatusPalette = StatusPalette(
    paid = Color(0xFF4ADE80),
    paidContainer = Color(0xFF14532D),
    onPaidContainer = Color(0xFFDCFCE7),
    partial = Color(0xFFFDBA74),
    partialContainer = Color(0xFF7C2D12),
    onPartialContainer = Color(0xFFFFEDD5),
    pending = Color(0xFFFCA5A5),
    pendingContainer = Color(0xFF7F1D1D),
    onPendingContainer = Color(0xFFFEE2E2),
    income = Color(0xFF5EEAD4),
    expense = Color(0xFFFDBA74)
)

val LocalStatusPalette = staticCompositionLocalOf { LightStatusPalette }
