package com.calculator.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.ui.graphics.Color

// Seed: Purple/Lavender (from screenshot)
val PurpleSeed = Color(0xFF7B5EA7)

// ----- Light scheme purple-seeded palette -----
val md_light_primary = Color(0xFF6B4FA0)
val md_light_onPrimary = Color(0xFFFFFFFF)
val md_light_primaryContainer = Color(0xFFE9DDFF)
val md_light_onPrimaryContainer = Color(0xFF24005A)

val md_light_secondary = Color(0xFF635B70)
val md_light_onSecondary = Color(0xFFFFFFFF)
val md_light_secondaryContainer = Color(0xFFE9DEF8)
val md_light_onSecondaryContainer = Color(0xFF1F182B)

val md_light_tertiary = Color(0xFF9C4060)
val md_light_onTertiary = Color(0xFFFFFFFF)
val md_light_tertiaryContainer = Color(0xFFFFD9E2)
val md_light_onTertiaryContainer = Color(0xFF3E001D)

val md_light_error = Color(0xFFBA1A1A)
val md_light_onError = Color(0xFFFFFFFF)
val md_light_errorContainer = Color(0xFFFFDAD6)
val md_light_onErrorContainer = Color(0xFF410002)

val md_light_background = Color(0xFFD8CCF0)
val md_light_onBackground = Color(0xFF1D1625)
val md_light_surface = Color(0xFFD8CCF0)
val md_light_onSurface = Color(0xFF1D1625)
val md_light_surfaceVariant = Color(0xFFE7E0EB)
val md_light_onSurfaceVariant = Color(0xFF49454E)
val md_light_outline = Color(0xFF7A757F)
val md_light_outlineVariant = Color(0xFFCAC4CF)
val md_light_inverseSurface = Color(0xFF322F35)
val md_light_inverseOnSurface = Color(0xFFF5EFF7)
val md_light_inversePrimary = Color(0xFFD0BCFF)
val md_light_surfaceTint = Color(0xFF6B4FA0)
val md_light_scrim = Color(0xFF000000)

// ----- Dark scheme purple-seeded palette -----
val md_dark_primary = Color(0xFFD0BCFF)
val md_dark_onPrimary = Color(0xFF381E72)
val md_dark_primaryContainer = Color(0xFF534587)
val md_dark_onPrimaryContainer = Color(0xFFE9DDFF)

val md_dark_secondary = Color(0xFFCCC2DC)
val md_dark_onSecondary = Color(0xFF332D41)
val md_dark_secondaryContainer = Color(0xFF4A4458)
val md_dark_onSecondaryContainer = Color(0xFFE9DEF8)

val md_dark_tertiary = Color(0xFFEFB8C8)
val md_dark_onTertiary = Color(0xFF4A1130)
val md_dark_tertiaryContainer = Color(0xFF7D2949)
val md_dark_onTertiaryContainer = Color(0xFFFFD9E2)

val md_dark_error = Color(0xFFFFB4AB)
val md_dark_onError = Color(0xFF690005)
val md_dark_errorContainer = Color(0xFF93000A)
val md_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_dark_background = Color(0xFF1D1625)
val md_dark_onBackground = Color(0xFFE7E0EB)
val md_dark_surface = Color(0xFF1D1625)
val md_dark_onSurface = Color(0xFFE7E0EB)
val md_dark_surfaceVariant = Color(0xFF49454E)
val md_dark_onSurfaceVariant = Color(0xFFCAC4CF)
val md_dark_outline = Color(0xFF948F99)
val md_dark_outlineVariant = Color(0xFF49454E)
val md_dark_inverseSurface = Color(0xFFE7E0EB)
val md_dark_inverseOnSurface = Color(0xFF322F35)
val md_dark_inversePrimary = Color(0xFF6B4FA0)
val md_dark_surfaceTint = Color(0xFFD0BCFF)
val md_dark_scrim = Color(0xFF000000)

// Light scheme: M3 Expressive base, overridden with purple palette
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val LightColorScheme = expressiveLightColorScheme().copy(
    primary = md_light_primary,
    onPrimary = md_light_onPrimary,
    primaryContainer = md_light_primaryContainer,
    onPrimaryContainer = md_light_onPrimaryContainer,
    secondary = md_light_secondary,
    onSecondary = md_light_onSecondary,
    secondaryContainer = md_light_secondaryContainer,
    onSecondaryContainer = md_light_onSecondaryContainer,
    tertiary = md_light_tertiary,
    onTertiary = md_light_onTertiary,
    tertiaryContainer = md_light_tertiaryContainer,
    onTertiaryContainer = md_light_onTertiaryContainer,
    error = md_light_error,
    onError = md_light_onError,
    errorContainer = md_light_errorContainer,
    onErrorContainer = md_light_onErrorContainer,
    background = md_light_background,
    onBackground = md_light_onBackground,
    surface = md_light_surface,
    onSurface = md_light_onSurface,
    surfaceVariant = md_light_surfaceVariant,
    onSurfaceVariant = md_light_onSurfaceVariant,
    outline = md_light_outline,
    outlineVariant = md_light_outlineVariant,
    inverseSurface = md_light_inverseSurface,
    inverseOnSurface = md_light_inverseOnSurface,
    inversePrimary = md_light_inversePrimary,
    surfaceTint = md_light_surfaceTint,
    scrim = md_light_scrim,
)

// Dark scheme with purple-tinted surfaces
val DarkColorScheme = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary = md_dark_secondary,
    onSecondary = md_dark_onSecondary,
    secondaryContainer = md_dark_secondaryContainer,
    onSecondaryContainer = md_dark_onSecondaryContainer,
    tertiary = md_dark_tertiary,
    onTertiary = md_dark_onTertiary,
    tertiaryContainer = md_dark_tertiaryContainer,
    onTertiaryContainer = md_dark_onTertiaryContainer,
    error = md_dark_error,
    onError = md_dark_onError,
    errorContainer = md_dark_errorContainer,
    onErrorContainer = md_dark_onErrorContainer,
    background = md_dark_background,
    onBackground = md_dark_onBackground,
    surface = md_dark_surface,
    onSurface = md_dark_onSurface,
    surfaceVariant = md_dark_surfaceVariant,
    onSurfaceVariant = md_dark_onSurfaceVariant,
    outline = md_dark_outline,
    outlineVariant = md_dark_outlineVariant,
    inverseSurface = md_dark_inverseSurface,
    inverseOnSurface = md_dark_inverseOnSurface,
    inversePrimary = md_dark_inversePrimary,
    surfaceTint = md_dark_surfaceTint,
    scrim = md_dark_scrim,
)

// Calculator-specific button colors (used directly, not from M3 scheme)
val EqualsButtonColor = Color(0xFFE87DA0)
val EqualsButtonContentColor = Color(0xFF1D1625)  // black/dark text on pink
val NumberButtonColor = Color(0xFFF0EAF7)
val NumberButtonContentColor = Color(0xFF1D1625)
val OperatorButtonColor = Color(0xFFCFC0E8)
val OperatorButtonContentColor = Color(0xFF1D1625)
val FunctionButtonColor = Color(0xFFBEA8DE)
val FunctionButtonContentColor = Color(0xFF1D1625)
val ACButtonColor = Color(0xFFD5D0DA)             // grey for AC
val ACButtonContentColor = Color(0xFF1D1625)
val DisplayBackground = Color(0xFFFFFFFF)
val DisplayBackgroundDark = Color(0xFF1D1625)
val ScientificTextColor = Color(0xFF4A3D5C)
val BackspaceIconColor = Color(0xFF3D2E52)

// Dark mode button colors
val EqualsButtonColorDark = Color(0xFFD06B8E)
val EqualsButtonContentColorDark = Color(0xFF1D1625)
val NumberButtonColorDark = Color(0xFF2E2639)
val NumberButtonContentColorDark = Color(0xFFE7E0EB)
val OperatorButtonColorDark = Color(0xFF3D3350)
val OperatorButtonContentColorDark = Color(0xFFE7E0EB)
val FunctionButtonColorDark = Color(0xFF4A3D5C)
val FunctionButtonContentColorDark = Color(0xFFE7E0EB)
val ACButtonColorDark = Color(0xFF3A3540)
val ACButtonContentColorDark = Color(0xFFE7E0EB)
val ScientificTextColorDark = Color(0xFFB0A6BE)
val BackspaceIconColorDark = Color(0xFFCAC4CF)
