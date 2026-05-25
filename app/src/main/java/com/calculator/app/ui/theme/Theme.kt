package com.calculator.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    oledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val finalColorScheme = if (oledBlack && darkTheme) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color(0xFF0A0A0A),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF050505),
            surfaceContainer = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF1A1A1A),
            surfaceContainerHighest = Color(0xFF2A2A2A),
        )
    } else {
        baseScheme
    }

    val motionScheme = MotionScheme.expressive()
    val spec = remember(motionScheme) { motionScheme.fastEffectsSpec<Color>() }

    // Animate only the visible-surface colors so theme switches feel smooth
    // without paying for 38 simultaneous color animations per recomposition.
    val animatedScheme = finalColorScheme.copy(
        surface = animateColorAsState(finalColorScheme.surface, spec).value,
        background = animateColorAsState(finalColorScheme.background, spec).value,
        surfaceContainer = animateColorAsState(finalColorScheme.surfaceContainer, spec).value,
        surfaceContainerHigh = animateColorAsState(finalColorScheme.surfaceContainerHigh, spec).value,
        surfaceContainerHighest = animateColorAsState(finalColorScheme.surfaceContainerHighest, spec).value,
        surfaceContainerLow = animateColorAsState(finalColorScheme.surfaceContainerLow, spec).value,
        surfaceContainerLowest = animateColorAsState(finalColorScheme.surfaceContainerLowest, spec).value,
        primary = animateColorAsState(finalColorScheme.primary, spec).value,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(darkTheme) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
            onDispose {}
        }
    }

    MaterialExpressiveTheme(
        colorScheme = animatedScheme,
        typography = CalculatorTypography,
        motionScheme = motionScheme,
        shapes = Shapes(),
        content = content,
    )
}
