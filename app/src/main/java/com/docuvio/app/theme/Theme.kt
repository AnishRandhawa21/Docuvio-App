package com.docuvio.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight


@Composable
fun LovelyPrintsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {

//    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
    Typography(
        bodyLarge = TextStyle(
            fontFamily = ImpactFont
        ),
        titleLarge = TextStyle(
            fontFamily = ImpactFont,
            fontWeight = FontWeight.Bold
        )
    )
    Typography(
        bodyLarge = TextStyle(
            fontFamily = Montserrat
        ),
        titleLarge = TextStyle(
            fontFamily = Montserrat,
            fontWeight = FontWeight.Bold
        )
    )

}
