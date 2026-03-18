package com.docuvio.app.theme


import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.docuvio.app.R
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),      // green cursor & selection
    onPrimary = Color.White,

    secondary = Color(0xFF1976D2),
    background = Cream,
    surface = Color.White,

    onBackground = AlmostBlack,
    onSurface = AlmostBlack
)
val Typography = Typography(
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

val ImpactFont = FontFamily(
    Font(R.font.impact, FontWeight.Normal)
)
val Montserrat = FontFamily(
    Font(R.font.montserrat_black, FontWeight.Black)
)
val Bebasneue = FontFamily(
    Font(R.font.bebasneue_regular, FontWeight.Black)
)
val Inter = FontFamily(
    Font(R.font.intertight_black, FontWeight.Normal)
)
val Thunder = FontFamily(
    Font(R.font.thunder, FontWeight.Normal)
)