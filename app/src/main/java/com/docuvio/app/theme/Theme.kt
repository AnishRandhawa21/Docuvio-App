package com.docuvio.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun LovelyPrintsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}