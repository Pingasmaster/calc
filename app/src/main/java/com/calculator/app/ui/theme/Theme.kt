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
        // The .copy() builds a brand-new ColorScheme on each call; memoize it so
        // unrelated recompositions of CalculatorTheme don't churn it every frame.
        remember(baseScheme) {
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
                // OPERATOR (secondaryContainer) + FUNCTION (tertiaryContainer)
                // buttons cover most of the grid; without these overrides they
                // stay bright purple/pink even in "OLED black" mode.
                secondaryContainer = Color(0xFF1A1A1A),
                onSecondaryContainer = Color(0xFFE7E0EB),
                tertiaryContainer = Color(0xFF1A1A1A),
                onTertiaryContainer = Color(0xFFE7E0EB),
            )
        }
    } else {
        baseScheme
    }

    val motionScheme = MotionScheme.expressive()
    val spec = remember(motionScheme) { motionScheme.fastEffectsSpec<Color>() }

    // Animate only the four most-visible role colors during a theme switch.
    // The surfaceContainer{Lowest,Low,High,Highest} variants are rarely on
    // screen long enough for the cross-fade to matter, so we let them pop
    // instantly and save four animateColorAsState subscriptions per frame.
    val animatedScheme = finalColorScheme.copy(
        surface = animateColorAsState(finalColorScheme.surface, spec).value,
        background = animateColorAsState(finalColorScheme.background, spec).value,
        surfaceContainer = animateColorAsState(finalColorScheme.surfaceContainer, spec).value,
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
