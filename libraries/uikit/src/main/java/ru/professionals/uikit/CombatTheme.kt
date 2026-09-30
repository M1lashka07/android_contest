package ru.professionals.uikit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Figma design tokens. Created 30-09-2026; author: training project (participant number pending). */
object CombatColors {
    val Pink = Color(0xFFFA5075)
    val PinkLight = Color(0xFFFF6480)
    val PinkDeep = Color(0xFFF22E63)
    val Ink = Color(0xFF030303)
    val Muted = Color(0xFF7D7D88)
    val Pale = Color(0xFFFFF0F4)
    val Blue = Color(0xFF4BA7F3)
}

private val Poppins = FontFamily(Font(R.font.poppins_regular), Font(R.font.poppins_bold, FontWeight.Bold))
private val CombatTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 33.sp),
    titleLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 21.sp),
    labelMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 15.sp)
)

/** Applies the light theme specified by the supplied Figma file. */
@Composable fun CombatTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = CombatColors.Pink, onPrimary = Color.White,
        background = Color.White, surface = Color.White, onSurface = CombatColors.Ink,
        onBackground = CombatColors.Ink, secondary = CombatColors.Blue, surfaceVariant = CombatColors.Pale),
        typography = CombatTypography, content = content)
}
