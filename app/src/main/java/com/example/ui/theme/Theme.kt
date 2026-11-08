package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkIdeColorScheme = darkColorScheme(
    primary = IdeAmberPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = IdeAmberContainer,
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = IdeCyanSecondary,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = IdeCyanContainer,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = IdeVioletAccent,
    onTertiary = Color(0xFF1E1B4B),
    background = IdeBgDark,
    onBackground = IdeTextPrimaryDark,
    surface = IdeSurfaceDark,
    onSurface = IdeTextPrimaryDark,
    surfaceVariant = IdeSurfaceElevatedDark,
    onSurfaceVariant = IdeTextSecondaryDark,
    outline = IdeBorderDark,
    error = IdeRubyError,
    onError = Color.White
)

private val LightIdeColorScheme = lightColorScheme(
    primary = IdeAmberPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = IdeCyanSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    background = IdeBgLight,
    onBackground = IdeTextPrimaryLight,
    surface = IdeSurfaceLight,
    onSurface = IdeTextPrimaryLight,
    surfaceVariant = IdeSurfaceElevatedLight,
    onSurfaceVariant = IdeTextSecondaryLight,
    outline = IdeBorderLight,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun UniversalJavaStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkIdeColorScheme else LightIdeColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
