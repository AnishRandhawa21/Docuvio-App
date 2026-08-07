package com.docuvio.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

val LightColorScheme = lightColorScheme(
    primary = SuccessGreen,
    onPrimary = White,
    secondary = Blue,
    onSecondary = White,
    tertiary = BrandOrange,
    onTertiary = AlmostBlack,

    background = Cream,
    onBackground = AlmostBlack,

    surface = White,
    onSurface = AlmostBlack,

    // Was previously undefined -> fell back to Material's default lavender-gray.
    // Now tied to your actual palette (warm cream tone, not stark white or purple).
    surfaceVariant = SurfaceCream,
    onSurfaceVariant = DarkGray,

    surfaceContainer = SurfaceCream,
    surfaceContainerHigh = SoftBeige,
    surfaceContainerLow = Cream,

    outline = LightGray,
    outlineVariant = SoftGreen,

    error = CoralRed,
    onError = White
)

val DarkColorScheme = darkColorScheme(
    primary = BrandLime,
    onPrimary = Black,
    secondary = Blue,
    onSecondary = White,
    tertiary = BrandOrange,
    onTertiary = Black,

    background = AlmostBlack,
    onBackground = Cream,

    surface = ShadowBrown,
    onSurface = Cream,

    // Dark-mode equivalent: a tonal step up from the base surface,
    // instead of Material's default dark purple-gray.
    surfaceVariant = Color(0xFF322D29),
    onSurfaceVariant = MediumGray,

    surfaceContainer = Color(0xFF2A2622),
    surfaceContainerHigh = Color(0xFF383330),
    surfaceContainerLow = ShadowBrown,

    outline = DarkGray,
    outlineVariant = ForestGreen,

    error = CoralRed,
    onError = White
)

@Composable
fun LovelyPrintsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}